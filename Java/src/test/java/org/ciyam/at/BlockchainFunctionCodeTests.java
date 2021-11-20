package org.ciyam.at;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.ciyam.at.test.ExecutableTest;
import org.ciyam.at.test.TestAPI;
import org.ciyam.at.test.TestAPI.TestAccount;
import org.ciyam.at.test.TestAPI.TestBlock;
import org.ciyam.at.test.TestAPI.TestTransaction;
import org.junit.Test;

public class BlockchainFunctionCodeTests extends ExecutableTest {

	@Test
	public void testGetBlockTimestamp() throws ExecutionException {
		// Grab block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT, blockTimestamp.blockHeight);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testMultipleGetBlockTimestamp() throws ExecutionException {
		int expectedBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;

		// Grab block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.STP_IMD.value);

		execute(true); // TestAPI's block height bumped prior to return

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		// Re-test
		++expectedBlockHeight;
		execute(true); // TestAPI's block height bumped prior to return

		blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetCreationTimestamp() throws ExecutionException {
		// Grab AT creation 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_CREATION_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", TestAPI.DEFAULT_AT_CREATION_BLOCK_HEIGHT, blockTimestamp.blockHeight);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetPreviousBlockTimestamp() throws ExecutionException {
		int expectedBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT - 1;

		// Grab previous block 'timestamp' and save into address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_PREVIOUS_BLOCK_TIMESTAMP.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		Timestamp blockTimestamp = new Timestamp(getData(0));
		assertEquals("Block timestamp incorrect", expectedBlockHeight, blockTimestamp.blockHeight);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutPreviousBlockHashIntoA() throws ExecutionException {
		// Generate some blocks containing transactions (but none to AT)
		TestBlock newBlock = api.generateBlockWithNonAtTransactions();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		newBlock = api.generateBlockWithNonAtTransactions();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Generate a block containing transaction to AT
		newBlock = api.generateBlockWithAtTransaction();
		api.addBlockToChain(newBlock);
		int previousBlockHeight = api.getCurrentBlockHeight();
		api.bumpCurrentBlockHeight();

		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_PREVIOUS_BLOCK_HASH_INTO_A.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		// previousBlockHeight - 1 because index into blockchain starts at 0, whereas block heights start at 1
		byte[] expectedBlockHash = api.blockchain.get(previousBlockHeight - 1).blockHash;

		byte[] aBytes = api.getA(state);
		assertTrue("Block hash mismatch", Arrays.equals(expectedBlockHash, aBytes));

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutTransactionAfterTimestampIntoA() throws ExecutionException {
		long initialTimestamp = Timestamp.toLong(TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate some blocks containing transactions (but none to AT)
		TestBlock newBlock = api.generateBlockWithNonAtTransactions();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		newBlock = api.generateBlockWithNonAtTransactions();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Generate a block containing transaction to AT
		newBlock = api.generateBlockWithAtTransaction();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		int currentBlockHeight = api.getCurrentBlockHeight();

		// Fetch transaction signature/hash after timestamp stored in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		TestTransaction transaction = api.getTransactionFromA(state);
		assertNotNull(transaction);

		Timestamp txTimestamp = new Timestamp(transaction.timestamp);
		assertEquals("Transaction hash mismatch", currentBlockHeight, txTimestamp.blockHeight);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutNoTransactionAfterTimestampIntoA() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate a block containing transaction to AT
		TestBlock newBlock = api.generateBlockWithAtTransaction();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		newBlock = api.generateBlockWithAtTransaction();
		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		long expectedTransactionsCount = 0;
		for (int blockHeight = initialBlockHeight + 1; blockHeight <= api.blockchain.size(); ++blockHeight) {
			TestBlock block = api.blockchain.get(blockHeight - 1);
			expectedTransactionsCount += block.transactions.stream().filter(transaction -> transaction.recipient.equals(TestAPI.AT_ADDRESS)).count();
		}

		// Count how many transactions after timestamp
		int targetPosition = 0x15;

		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.CHECK_A_IS_ZERO.value).putInt(1);
		int bzrPosition = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(1).put((byte) (targetPosition - bzrPosition));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		assertEquals("targetPosition incorrect", targetPosition, codeByteBuffer.position());
		// Update latest timestamp in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(0);
		// Increment transactions count
		codeByteBuffer.put(OpCode.INC_DAT.value).putInt(2);
		// Loop again
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		execute(true);

		long transactionsCount = getData(2);
		assertEquals("Transaction count incorrect", expectedTransactionsCount, transactionsCount);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetTypeFromTxInA() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate new block containing 2 transactions to AT, one PAYMENT, one MESSAGE
		TestBlock newBlock = api.generateEmptyBlock();

		String sender = "Bystander";
		String recipient = TestAPI.AT_ADDRESS;

		TestTransaction paymentTx = api.generateTransaction(sender, recipient, API.ATTransactionType.PAYMENT);
		newBlock.transactions.add(paymentTx);

		TestTransaction messageTx = api.generateTransaction(sender, recipient, API.ATTransactionType.MESSAGE);
		newBlock.transactions.add(messageTx);

		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction type into address 1
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TYPE_FROM_TX_IN_A.value).putInt(1);
		// Update latest timestamp in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(0);

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction type into address 2
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TYPE_FROM_TX_IN_A.value).putInt(2);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		long paymentTxType = getData(1);
		assertEquals("Payment tx type mismatch", paymentTx.txType.value, paymentTxType);

		long messageTxType = getData(2);
		assertEquals("Message tx type mismatch", messageTx.txType.value, messageTxType);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetAmountFromTxInA() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Generate new block containing 2 transactions to AT, one PAYMENT, one MESSAGE
		TestBlock newBlock = api.generateEmptyBlock();

		String sender = "Bystander";
		String recipient = TestAPI.AT_ADDRESS;

		TestTransaction paymentTx = api.generateTransaction(sender, recipient, API.ATTransactionType.PAYMENT);
		newBlock.transactions.add(paymentTx);

		TestTransaction messageTx = api.generateTransaction(sender, recipient, API.ATTransactionType.MESSAGE);
		newBlock.transactions.add(messageTx);

		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction's amount into address 1
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_AMOUNT_FROM_TX_IN_A.value).putInt(1);
		// Update latest timestamp in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(0);

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction's amount into address 2
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_AMOUNT_FROM_TX_IN_A.value).putInt(2);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		long paymentTxAmount = getData(1);
		assertEquals("Payment tx amount mismatch", paymentTx.amount, paymentTxAmount);

		long messageTxAmount = getData(2);
		assertEquals("Message tx amount mismatch", messageTx.amount, messageTxAmount);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testRandom() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(Timestamp.toLong(api.getCurrentBlockHeight(), 0));
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GENERATE_RANDOM_USING_TX_IN_A.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// Generate a block containing transaction to AT
		api.generateBlockWithAtTransaction();

		execute(false);

		assertNotEquals("Random wasn't generated", 0L, getData(1));
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutMessageFromTxInAIntoB() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Where to save message (in B) from payment tx
		dataByteBuffer.putLong(4L);

		// Where to save message (in B) from message tx
		dataByteBuffer.putLong(8L);

		// Generate new block containing 2 transactions to AT, one PAYMENT, one MESSAGE
		TestBlock newBlock = api.generateEmptyBlock();

		String sender = "Bystander";
		String recipient = TestAPI.AT_ADDRESS;

		TestTransaction paymentTx = api.generateTransaction(sender, recipient, API.ATTransactionType.PAYMENT);
		newBlock.transactions.add(paymentTx);

		TestTransaction messageTx = api.generateTransaction(sender, recipient, API.ATTransactionType.MESSAGE);
		newBlock.transactions.add(messageTx);

		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction's message into addresses 4 to 7
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_MESSAGE_FROM_TX_IN_A_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.GET_B_IND.value).putInt(1);
		// Update latest timestamp in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A.value).putInt(0);

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction's message into addresses 4 to 7
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_MESSAGE_FROM_TX_IN_A_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.GET_B_IND.value).putInt(2);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		byte[] actualMessage = new byte[32];

		byte[] expectedMessage = new byte[32]; // Blank for non-message transactions
		getDataBytes(4, actualMessage);
		assertTrue("Payment tx message mismatch", Arrays.equals(expectedMessage, actualMessage));

		expectedMessage = messageTx.message;
		getDataBytes(8, actualMessage);
		assertTrue("Message tx message mismatch", Arrays.equals(expectedMessage, actualMessage));

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutAddressFromTxInAIntoB() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		// Where to save address (in B) from tx
		dataByteBuffer.putLong(4L);

		// Generate new block containing a transaction to AT
		TestBlock newBlock = api.generateEmptyBlock();

		String sender = "Bystander";
		String recipient = TestAPI.AT_ADDRESS;

		TestTransaction messageTx = api.generateTransaction(sender, recipient, API.ATTransactionType.MESSAGE);
		newBlock.transactions.add(messageTx);

		api.addBlockToChain(newBlock);
		api.bumpCurrentBlockHeight();

		// Get transaction after timestamp held in address 0
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A.value).putInt(0);
		// Save transaction's sender into addresses 4 to 7
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_ADDRESS_FROM_TX_IN_A_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.GET_B_IND.value).putInt(1);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		byte[] expectedSenderBytes = TestAPI.encodeAddress(sender);
		byte[] actualSenderBytes = new byte[32];
		getDataBytes(4, actualSenderBytes);
		assertTrue("Sender address bytes mismatch", Arrays.equals(expectedSenderBytes, actualSenderBytes));

		String expectedSender = sender;
		String actualSender = TestAPI.decodeAddress(actualSenderBytes);
		assertEquals("Sender address string mismatch", expectedSender, actualSender);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPutCreatorIntoB() throws ExecutionException {
		// Where to save creator address (in B)
		dataByteBuffer.putLong(4L);

		// Save creator's address into data addresses 4 to 7
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_CREATOR_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.GET_B_IND.value).putInt(0);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		byte[] expectedAtCreatorBytes = TestAPI.encodeAddress(TestAPI.AT_CREATOR_ADDRESS);
		byte[] actualAtCreatorBytes = new byte[32];
		getDataBytes(4, actualAtCreatorBytes);
		assertTrue("AT creator address bytes mismatch", Arrays.equals(expectedAtCreatorBytes, actualAtCreatorBytes));

		String expectedAtCreator = TestAPI.AT_CREATOR_ADDRESS;
		String actualAtCreator = TestAPI.decodeAddress(actualAtCreatorBytes);
		assertEquals("AT creator address string mismatch", expectedAtCreator, actualAtCreator);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetCurrentBalance() throws ExecutionException {
		// Number of bits to shift right
		dataByteBuffer.putLong(1L);

		// Save current balance into address 1
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_CURRENT_BALANCE.value).putInt(1);
		// Copy balance from address 1 into address 2
		codeByteBuffer.put(OpCode.SET_DAT.value).putInt(2).putInt(1);
		// Halve balance in address 2
		codeByteBuffer.put(OpCode.SHR_DAT.value).putInt(2).putInt(0);
		// Pay amount in address 2 to creator (via B)
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PUT_CREATOR_INTO_B.value);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PAY_TO_ADDRESS_IN_B.value).putInt(2);
		// Save new current balance into address 3
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_CURRENT_BALANCE.value).putInt(3);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		final long initialBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;

		execute(true);

		long expectedBalance = initialBalance - TestAPI.STEPS_PER_FUNCTION_CALL /* GET_CURRENT_BALANCE */;
		assertEquals("Initial 'current balance' mismatch", expectedBalance, getData(1));

		final long amount = expectedBalance >>> 1;
		expectedBalance -= 1 /* SET_DAT */
				+ 1 /* SHR_DAT */
				+ TestAPI.STEPS_PER_FUNCTION_CALL /* PUT_CREATOR_INTO_B */
				+ TestAPI.STEPS_PER_FUNCTION_CALL /* PAY_TO_ADDRESS_IN_B */
				+ TestAPI.STEPS_PER_FUNCTION_CALL /* GET_CURRENT_BALANCE */
				+ amount;

		assertEquals("Final 'current balance' mismatch", expectedBalance, getData(3));

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testGetPreviousBalance() throws ExecutionException {
		// Save previous balance into address 1
		codeByteBuffer.put(OpCode.EXT_FUN_RET.value).putShort(FunctionCode.GET_PREVIOUS_BALANCE.value).putInt(1);
		// Done
		codeByteBuffer.put(OpCode.STP_IMD.value);

		final long initialBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;

		execute(true);

		long expectedBalance = initialBalance;
		assertEquals("Initial 'previous balance' mismatch", expectedBalance, getData(1));

		execute(true);

		expectedBalance -= 1 /* STP_IMD */ + TestAPI.STEPS_PER_FUNCTION_CALL /* GET_CURRENT_BALANCE */;

		assertEquals("Final 'previous balance' mismatch", expectedBalance, getData(1));

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPayToAddressInB() throws ExecutionException {
		final long amount = 123L;
		TestAccount recipient = api.accounts.get("Bystander");

		// Where recipient address is stored
		final long addressPosition = 2L;
		dataByteBuffer.putLong(addressPosition);

		// Amount to send to recipient
		dataByteBuffer.putLong(amount);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(0);
		// Pay amount in address 1 to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PAY_TO_ADDRESS_IN_B.value).putInt(1);
		// STOP, not finish, so we retain balance instead of sending leftover to AT creator
		codeByteBuffer.put(OpCode.STP_IMD.value);

		final long initialAtBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		final long initialRecipientBalance = recipient.balance;

		execute(true);

		long expectedBalance = initialAtBalance - amount;
		long actualBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		assertTrue("Final AT balance mismatch", actualBalance <= expectedBalance && actualBalance > 0);

		expectedBalance = initialRecipientBalance + amount;
		actualBalance = recipient.balance;
		assertEquals("Final recipient balance mismatch", expectedBalance, actualBalance);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPayToAddressInBexcessive() throws ExecutionException {
		final long amount = 999999999L; // More than AT's balance
		TestAccount recipient = api.accounts.get("Bystander");

		// Where recipient address is stored
		final long addressPosition = 2L;
		dataByteBuffer.putLong(addressPosition);

		// Amount to send to recipient
		dataByteBuffer.putLong(amount);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(0);
		// Pay amount in address 1 to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.PAY_TO_ADDRESS_IN_B.value).putInt(1);
		// STOP, not finish, so we retain balance instead of sending leftover to AT creator
		codeByteBuffer.put(OpCode.STP_IMD.value);

		final long initialAtBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		final long initialRecipientBalance = recipient.balance;

		execute(true);

		long expectedAmount = initialAtBalance
				- TestAPI.STEPS_PER_FUNCTION_CALL /* SET_B_IND */
				- TestAPI.STEPS_PER_FUNCTION_CALL /* PAY_TO_ADDRESS_IN_B */;

		long expectedBalance = 0L;
		long actualBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		assertEquals("Final AT balance mismatch", expectedBalance, actualBalance);

		expectedBalance = initialRecipientBalance + expectedAmount;
		actualBalance = recipient.balance;
		assertEquals("Final recipient balance mismatch", expectedBalance, actualBalance);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPayAllToAddressInB() throws ExecutionException {
		TestAccount recipient = api.accounts.get("Bystander");

		// Where recipient address is stored
		final long addressPosition = 1L;
		dataByteBuffer.putLong(addressPosition);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(0);
		// Pay all amount to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PAY_ALL_TO_ADDRESS_IN_B.value);
		// STOP, not finish, so we retain balance instead of sending leftover to AT creator
		codeByteBuffer.put(OpCode.STP_IMD.value);

		final long initialAtBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		final long initialRecipientBalance = recipient.balance;

		execute(true);

		long expectedAmount = initialAtBalance
				- TestAPI.STEPS_PER_FUNCTION_CALL /* SET_B_IND */
				- TestAPI.STEPS_PER_FUNCTION_CALL /* PAY_ALL_TO_ADDRESS_IN_B */;

		long expectedBalance = 0L;
		long actualBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		assertEquals("Final AT balance mismatch", expectedBalance, actualBalance);

		expectedBalance = initialRecipientBalance + expectedAmount;
		actualBalance = recipient.balance;
		assertEquals("Final recipient balance mismatch", expectedBalance, actualBalance);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPayPreviousToAddressInB() throws ExecutionException {
		TestAccount recipient = api.accounts.get("Bystander");

		// Where recipient address is stored
		final long addressPosition = 1L;
		dataByteBuffer.putLong(addressPosition);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(0);
		// Pay previous balance to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PAY_PREVIOUS_TO_ADDRESS_IN_B.value);
		// STOP, not finish, so we retain balance instead of sending leftover to AT creator
		codeByteBuffer.put(OpCode.STP_IMD.value);

		final long initialAtBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		final long initialRecipientBalance = recipient.balance;

		execute(true);

		long expectedAmount = initialAtBalance
				- TestAPI.STEPS_PER_FUNCTION_CALL /* SET_B_IND */
				- TestAPI.STEPS_PER_FUNCTION_CALL /* PAY_PREVIOUS_TO_ADDRESS_IN_B */;

		long expectedBalance = 0L;
		long actualBalance = api.accounts.get(TestAPI.AT_ADDRESS).balance;
		assertEquals("Final AT balance mismatch", expectedBalance, actualBalance);

		expectedBalance = initialRecipientBalance + expectedAmount;
		actualBalance = recipient.balance;
		assertEquals("Final recipient balance mismatch", expectedBalance, actualBalance);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPayPreviousToAddressInBextra() throws ExecutionException {
		TestAccount recipient = api.accounts.get("Bystander");

		// Where recipient address is stored
		final long addressPosition = 1L;
		dataByteBuffer.putLong(addressPosition);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Sleep until next block
		codeByteBuffer.put(OpCode.SLP_IMD.value);
		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(0);
		// Pay previous balance to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.PAY_PREVIOUS_TO_ADDRESS_IN_B.value);
		// STOP, not finish, so we retain balance instead of sending leftover to AT creator
		codeByteBuffer.put(OpCode.STP_IMD.value);

		execute(true);

		final TestAccount atAccount = api.accounts.get(TestAPI.AT_ADDRESS);

		final long previousAtBalance = atAccount.balance;
		final long initialRecipientBalance = recipient.balance;

		// Simulate AT receiving a payment (in excess of cost of running AT for one round)
		final long incomingAtPayment = 25000L;
		atAccount.balance += incomingAtPayment;

		execute(true);

		long expectedBalance = incomingAtPayment
				- TestAPI.STEPS_PER_FUNCTION_CALL /* SET_B_IND */
				- TestAPI.STEPS_PER_FUNCTION_CALL /* PAY_PREVIOUS_TO_ADDRESS_IN_B */
				- 1 /* STP_IMD */;
		long actualBalance = atAccount.balance;
		assertEquals("Final AT balance mismatch", expectedBalance, actualBalance);

		expectedBalance = initialRecipientBalance + previousAtBalance;
		actualBalance = recipient.balance;
		assertEquals("Final recipient balance mismatch", expectedBalance, actualBalance);

		assertFalse(state.hadFatalError());
	}

	@Test
	public void testMessageAToAddressInB() throws ExecutionException {
		Random random = new Random();

		byte[] message = new byte[32];
		random.nextBytes(message);

		TestAccount recipient = api.accounts.get("Bystander");

		// Where message is stored
		final long messagePosition = 2L;
		dataByteBuffer.putLong(messagePosition);

		// Where recipient address is stored
		final long addressPosition = 6L;
		dataByteBuffer.putLong(addressPosition);

		// Message
		assertEquals(messagePosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(message);

		// Recipient address
		assertEquals(addressPosition * MachineState.VALUE_SIZE, dataByteBuffer.position());
		dataByteBuffer.put(TestAPI.encodeAddress(recipient.address));

		// Copy message into A
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_A_IND.value).putInt(0);
		// Copy recipient address into B
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.SET_B_IND.value).putInt(1);
		// Send message (in A) to recipient (via B)
		codeByteBuffer.put(OpCode.EXT_FUN.value).putShort(FunctionCode.MESSAGE_A_TO_ADDRESS_IN_B.value);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertEquals("Recipient message count incorrect", 1, recipient.messages.size());

		byte[] actualMessage = recipient.messages.get(0);
		assertTrue("Recipient message incorrect", Arrays.equals(message, actualMessage));

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testAddMinutesToTimestamp() throws ExecutionException {
		int initialBlockHeight = TestAPI.DEFAULT_INITIAL_BLOCK_HEIGHT;
		long initialTimestamp = Timestamp.toLong(initialBlockHeight, 0);
		dataByteBuffer.putLong(initialTimestamp);

		long minutes = 34L;
		dataByteBuffer.putLong(minutes);

		// Add minutes (from address 1) to timestamp (in address 0) and store the result in address 2
		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(FunctionCode.ADD_MINUTES_TO_TIMESTAMP.value).putInt(2).putInt(0).putInt(1);
		// Done
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		int expectedBlockHeight = initialBlockHeight + ((int) minutes * 60 / TestAPI.BLOCK_PERIOD);
		long expectedTimestamp = Timestamp.toLong(expectedBlockHeight, 0);
		long actualTimestamp = getData(2);
		assertEquals("Expected timestamp incorrect", expectedTimestamp, actualTimestamp);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

}
