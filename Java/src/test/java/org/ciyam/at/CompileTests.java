package org.ciyam.at;

import static org.ciyam.at.OpCode.calcOffset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.ciyam.at.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CompileTests {

	private ByteBuffer codeByteBuffer;

	@Before
	public void before() {
		this.codeByteBuffer = ByteBuffer.allocate(512);
	}

	@After
	public void after() {
		this.codeByteBuffer = null;
	}

	@Test
	public void testSimpleCompile() throws CompilationException {
		int address = 1234;
		long value = System.currentTimeMillis();

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(address).putLong(value);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = OpCode.SET_VAL.compile(address, value);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testWideningCompile() throws CompilationException {
		int address = 1234;
		int value = 9999;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(address).putLong(value);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = OpCode.SET_VAL.compile(address, value);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testBranchCompile() throws CompilationException {
		int address = 1234;
		byte offset = (byte) 16;

		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(address).put(offset);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = OpCode.BZR_DAT.compile(address, offset);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testBranchNarrowingCompile() throws CompilationException {
		int address = 1234;
		int offset = 16; // fits within byte

		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(address).put((byte) offset);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = OpCode.BZR_DAT.compile(address, offset);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testBranchCompileFailure() throws CompilationException {
		int address = 1234;
		int offset = 9999; // larger than a byte

		try {
			codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(address).put((byte) offset);
		} catch (Throwable t) {
			fail("Narrowing to byte would silently fail with old code");
		}

		try {
			byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
			byte[] actualBytes = OpCode.BZR_DAT.compile(address, offset);

			assertTrue(Arrays.equals(expectedBytes, actualBytes));
		} catch (CompilationException e) {
			// this is to be expected as offset is too big to fit into byte
			System.out.println("Expected error: " + e.getMessage());
			return;
		}

		fail("Narrowing to byte should have caused exception");
	}

	@Test
	public void testFunctionCompile() throws CompilationException {
		int addrValue1 = 1234;
		int addrValue2 = 5678;
		int addrResult = 9999;

		codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.value).putShort(FunctionCode.ADD_MINUTES_TO_TIMESTAMP.value).putInt(addrResult).putInt(addrValue1).putInt(addrValue2);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = OpCode.EXT_FUN_RET_DAT_2.compile(FunctionCode.ADD_MINUTES_TO_TIMESTAMP, addrResult, addrValue1, addrValue2);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testTwoPassCompile() throws CompilationException {
		int addrData = 0;
		Integer actualTarget = null;
		int expectedTarget = 0x06;

		// Old version
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(addrData).put((byte) expectedTarget);

		// Two-pass version
		ByteBuffer compileBuffer = ByteBuffer.allocate(512);
		for (int pass = 0; pass < 2; ++pass) {
			compileBuffer.clear();

			compileBuffer.put(OpCode.BZR_DAT.compile(addrData, calcOffset(compileBuffer, actualTarget)));

			actualTarget = compileBuffer.position();
		}

		assertEquals(expectedTarget, (int) actualTarget);

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);
		byte[] actualBytes = TestUtils.getBytes(compileBuffer);

		assertTrue(Arrays.equals(expectedBytes, actualBytes));
	}

	@Test
	public void testTwoPassBranchCompileFailure() throws CompilationException {
		int addrData = 0;
		Integer actualTarget = null;
		int expectedTarget = 0x06;

		// Old version
		codeByteBuffer.put(OpCode.BZR_DAT.value).putInt(addrData).put((byte) expectedTarget);

		// Two-pass version
		ByteBuffer compileBuffer = ByteBuffer.allocate(512);
		for (int pass = 0; pass < 2; ++pass) {
			compileBuffer.clear();

			try {
				compileBuffer.put(OpCode.BZR_DAT.compile(addrData, calcOffset(compileBuffer, actualTarget)));
			} catch (CompilationException e) {
				// expected
				return;
			}

			actualTarget = compileBuffer.position() + 333; // wider than a signed byte
		}

		fail("CompilationException should have been thrown");
	}

	@SuppressWarnings("unused")
	@Test
	public void testComplexCompile() throws CompilationException {
		// Labels for data segment addresses
		int addrCounter = 0;
		// Constants (with corresponding dataByteBuffer.put*() calls below)
		final int addrHashPart1 = addrCounter++;
		final int addrHashPart2 = addrCounter++;
		final int addrHashPart3 = addrCounter++;
		final int addrHashPart4 = addrCounter++;
		final int addrHashIndex = addrCounter++;
		final int addrAddressPart1 = addrCounter++;
		final int addrAddressPart2 = addrCounter++;
		final int addrAddressPart3 = addrCounter++;
		final int addrAddressPart4 = addrCounter++;
		final int addrAddressIndex = addrCounter++;
		final int addrRefundMinutes = addrCounter++;
		final int addrHashTempIndex = addrCounter++;
		final int addrHashTempLength = addrCounter++;
		final int addrInitialPayoutAmount = addrCounter++;
		final int addrExpectedTxType = addrCounter++;
		final int addrAddressTempIndex = addrCounter++;
		// Variables
		final int addrRefundTimestamp = addrCounter++;
		final int addrLastTimestamp = addrCounter++;
		final int addrBlockTimestamp = addrCounter++;
		final int addrTxType = addrCounter++;
		final int addrComparator = addrCounter++;
		final int addrAddressTemp1 = addrCounter++;
		final int addrAddressTemp2 = addrCounter++;
		final int addrAddressTemp3 = addrCounter++;
		final int addrAddressTemp4 = addrCounter++;
		final int addrHashTemp1 = addrCounter++;
		final int addrHashTemp2 = addrCounter++;
		final int addrHashTemp3 = addrCounter++;
		final int addrHashTemp4 = addrCounter++;

		Integer labelTxLoop = null;
		Integer labelRefund = null;
		Integer labelCheckTx = null;

		// Two-pass version
		for (int pass = 0; pass < 2; ++pass) {
			codeByteBuffer.clear();

			/* Initialization */

			// Use AT creation 'timestamp' as starting point for finding transactions sent to AT
			codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_CREATION_TIMESTAMP, addrLastTimestamp));
			// Calculate refund 'timestamp' by adding minutes to above 'timestamp', then save into addrRefundTimestamp
			codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.compile(FunctionCode.ADD_MINUTES_TO_TIMESTAMP, addrRefundTimestamp, addrLastTimestamp, addrRefundMinutes));

			// Load recipient's address into B register
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.SET_B_IND, addrAddressIndex));
			// Send initial payment to recipient so they have enough funds to message AT if all goes well
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.PAY_TO_ADDRESS_IN_B, addrInitialPayoutAmount));

			// Set restart position to after this opcode
			codeByteBuffer.put(OpCode.SET_PCS.compile());

			/* Main loop */

			// Fetch current block 'timestamp'
			codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_BLOCK_TIMESTAMP, addrBlockTimestamp));
			// If we're not past refund 'timestamp' then look for next transaction
			codeByteBuffer.put(OpCode.BLT_DAT.compile(addrBlockTimestamp, addrRefundTimestamp, calcOffset(codeByteBuffer, labelTxLoop)));
			// We're past refund 'timestamp' so go refund everything back to AT creator
			codeByteBuffer.put(OpCode.JMP_ADR.compile(labelRefund == null ? 0 : labelRefund));

			/* Transaction processing loop */
			labelTxLoop = codeByteBuffer.position();

			// Find next transaction to this AT since the last one (if any)
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.PUT_TX_AFTER_TIMESTAMP_INTO_A, addrLastTimestamp));
			// If no transaction found, A will be zero. If A is zero, set addrComparator to 1, otherwise 0.
			codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.CHECK_A_IS_ZERO, addrComparator));
			// If addrComparator is zero (i.e. A is non-zero, transaction was found) then go check transaction
			codeByteBuffer.put(OpCode.BZR_DAT.compile(addrComparator, calcOffset(codeByteBuffer, labelCheckTx)));
			// Stop and wait for next block
			codeByteBuffer.put(OpCode.STP_IMD.compile());

			/* Check transaction */
			labelCheckTx = codeByteBuffer.position();

			// Update our 'last found transaction's timestamp' using 'timestamp' from transaction
			codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_TIMESTAMP_FROM_TX_IN_A, addrLastTimestamp));
			// Extract transaction type (message/payment) from transaction and save type in addrTxType
			codeByteBuffer.put(OpCode.EXT_FUN_RET.compile(FunctionCode.GET_TYPE_FROM_TX_IN_A, addrTxType));
			// If transaction type is not MESSAGE type then go look for another transaction
			codeByteBuffer.put(OpCode.BNE_DAT.compile(addrTxType, addrExpectedTxType, calcOffset(codeByteBuffer, labelTxLoop)));

			/* Check transaction's sender */

			// Extract sender address from transaction into B register
			codeByteBuffer.put(OpCode.EXT_FUN.compile(FunctionCode.PUT_ADDRESS_FROM_TX_IN_A_INTO_B));
			// Save B register into data segment starting at addrAddressTemp1 (as pointed to by addrAddressTempIndex)
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.GET_B_IND, addrAddressTempIndex));
			// Compare each part of transaction's sender's address with expected address. If they don't match, look for another transaction.
			codeByteBuffer.put(OpCode.BNE_DAT.compile(addrAddressTemp1, addrAddressPart1, calcOffset(codeByteBuffer, labelTxLoop)));
			codeByteBuffer.put(OpCode.BNE_DAT.compile(addrAddressTemp2, addrAddressPart2, calcOffset(codeByteBuffer, labelTxLoop)));
			codeByteBuffer.put(OpCode.BNE_DAT.compile(addrAddressTemp3, addrAddressPart3, calcOffset(codeByteBuffer, labelTxLoop)));
			codeByteBuffer.put(OpCode.BNE_DAT.compile(addrAddressTemp4, addrAddressPart4, calcOffset(codeByteBuffer, labelTxLoop)));

			/* Check 'secret' in transaction's message */

			// Extract message from transaction into B register
			codeByteBuffer.put(OpCode.EXT_FUN.compile(FunctionCode.PUT_MESSAGE_FROM_TX_IN_A_INTO_B));
			// Save B register into data segment starting at addrHashTemp1 (as pointed to by addrHashTempIndex)
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.GET_B_IND, addrHashTempIndex));
			// Load B register with expected hash result (as pointed to by addrHashIndex)
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.SET_B_IND, addrHashIndex));
			// Perform HASH160 using source data at addrHashTemp1 through addrHashTemp4. (Location and length specified via addrHashTempIndex and addrHashTemplength).
			// Save the equality result (1 if they match, 0 otherwise) into addrComparator.
			codeByteBuffer.put(OpCode.EXT_FUN_RET_DAT_2.compile(FunctionCode.CHECK_HASH160_WITH_B, addrComparator, addrHashTempIndex, addrHashTempLength));
			// If hashes don't match, addrComparator will be zero so go find another transaction
			codeByteBuffer.put(OpCode.BZR_DAT.compile(addrComparator, calcOffset(codeByteBuffer, labelTxLoop)));

			/* Success! Pay balance to intended recipient */

			// Load B register with intended recipient address (as pointed to by addrAddressIndex)
			codeByteBuffer.put(OpCode.EXT_FUN_DAT.compile(FunctionCode.SET_B_IND, addrAddressIndex));
			// Pay AT's balance to recipient
			codeByteBuffer.put(OpCode.EXT_FUN.compile(FunctionCode.PAY_ALL_TO_ADDRESS_IN_B));
			// We're finished forever
			codeByteBuffer.put(OpCode.FIN_IMD.compile());

			/* Refund balance back to AT creator */
			labelRefund = codeByteBuffer.position();

			// Load B register with AT creator's address.
			codeByteBuffer.put(OpCode.EXT_FUN.compile(FunctionCode.PUT_CREATOR_INTO_B));
			// Pay AT's balance back to AT's creator.
			codeByteBuffer.put(OpCode.EXT_FUN.compile(FunctionCode.PAY_ALL_TO_ADDRESS_IN_B));
			// We're finished forever
			codeByteBuffer.put(OpCode.FIN_IMD.compile());
		}

		byte[] expectedBytes = TestUtils.getBytes(codeByteBuffer);

		assertTrue(expectedBytes.length > 0);
	}

}
