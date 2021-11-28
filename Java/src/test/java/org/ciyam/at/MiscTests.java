package org.ciyam.at;

import static org.junit.Assert.*;

import org.ciyam.at.test.ExecutableTest;
import org.ciyam.at.test.TestAPI;
import org.ciyam.at.test.TestUtils;
import org.junit.Test;

public class MiscTests extends ExecutableTest {

	@Test
	public void testSimpleCode() throws ExecutionException {
		long testValue = 8888L;
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(testValue);
		codeByteBuffer.put(OpCode.EXT_FUN_DAT.value).putShort(FunctionCode.ECHO.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", testValue, getData(0));
	}

	@Test
	public void testInvalidOpCode() throws ExecutionException {
		codeByteBuffer.put((byte) 0xdd);

		execute(true);

		assertTrue(state.isFinished());
		assertTrue(state.hadFatalError());
	}

	@Test
	public void testFreeze() throws ExecutionException {
		// Choose initial balance so it used up before max-steps-per-round triggers
		long initialBalance = 5L;
		api.accounts.get(TestAPI.AT_ADDRESS).balance = initialBalance;

		// Infinite loop
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		// Test a few rounds to make sure AT is frozen and stays frozen
		for (int i = 0; i < 3; ++i) {
			execute(true);

			assertTrue(state.isFrozen());

			Long frozenBalance = state.getFrozenBalance();
			assertNotNull(frozenBalance);
		}
	}

	@Test
	public void testUnfreeze() throws ExecutionException {
		// Choose initial balance so it used up before max-steps-per-round triggers
		long initialBalance = 5L;
		api.setCurrentBalance(initialBalance);

		// Infinite loop
		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(0);

		// Execute to make sure AT is frozen and stays frozen
		execute(true);

		assertTrue(state.isFrozen());

		Long frozenBalance = state.getFrozenBalance();
		assertNotNull(frozenBalance);

		// Send payment to AT to allow unfreezing
		// Payment needs to be enough to trigger max-steps-per-round so we can detect unfreezing
		api.setCurrentBalance(TestAPI.MAX_STEPS_PER_ROUND * api.getFeePerStep() * 2);

		// Execute AT
		execute(true);

		// We expect AT to be sleeping, not frozen
		assertFalse(state.isFrozen());

		frozenBalance = state.getFrozenBalance();
		assertNull(frozenBalance);

		assertTrue(state.isSleeping());
	}

	@Test
	public void testMinActivation() throws ExecutionException {
		// Make sure minimum activation amount is greater than initial balance
		long minActivationAmount = TestAPI.DEFAULT_INITIAL_BALANCE * 2L;

		byte[] headerBytes = TestUtils.toHeaderBytes(TestUtils.VERSION, TestUtils.NUM_CODE_PAGES, TestUtils.NUM_DATA_PAGES, TestUtils.NUM_CALL_STACK_PAGES, TestUtils.NUM_USER_STACK_PAGES, minActivationAmount);
		byte[] codeBytes = codeByteBuffer.array();
		byte[] dataBytes = new byte[0];

		state = new MachineState(api, loggerFactory, headerBytes, codeBytes, dataBytes);

		assertTrue(state.isFrozen());
		assertEquals((Long) (minActivationAmount - 1L), state.getFrozenBalance());
	}

	@Test
	public void testDataAddressBounds() throws ExecutionException {
		// Last possible valid address in data segment
		int lastDataAddress = (dataByteBuffer.limit() / MachineState.VALUE_SIZE) - 1;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(lastDataAddress).putLong(8888L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

}
