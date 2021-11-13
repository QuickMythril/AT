package org.ciyam.at;

import static org.ciyam.at.test.TestUtils.hexToBytes;
import static org.junit.Assert.*;

import org.ciyam.at.test.ExecutableTest;
import org.junit.Test;

public class OpCodeTests extends ExecutableTest {

	@Test
	public void testNOP() throws ExecutionException {
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());

		// Check data unchanged
		for (int i = 0; i < 0x0020; ++i)
			assertEquals(0L, getData(i));
	}

	@Test
	public void testJMP_ADR() throws ExecutionException {
		int targetAddr = 0x12;

		codeByteBuffer.put(OpCode.JMP_ADR.value).putInt(targetAddr);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1L);

		// targetAddr:
		assertEquals(targetAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(2L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 2L, getData(0));
	}

	@Test
	public void testSLP_DAT() throws ExecutionException {
		int targetBlockHeight = api.getCurrentBlockHeight() + 5;

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(targetBlockHeight);
		codeByteBuffer.put(OpCode.SLP_DAT.value).putInt(1);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// one round only, to check AT has started sleeping
		execute(true);

		assertTrue(state.isSleeping());
		assertFalse(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Sleep-until block height incorrect", targetBlockHeight, state.getSleepUntilHeight().intValue());

		// continue sleeping until AT awakens
		execute(false);

		assertEquals("Reawaken block height incorrect", targetBlockHeight, state.getCurrentBlockHeight());

		assertFalse(state.isSleeping());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Sleep-until block height incorrect", null, state.getSleepUntilHeight());
	}

	@Test
	public void testFIZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.isSleeping());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testFIZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.FIZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.SLP_IMD.value);

		execute(true);

		assertFalse(state.isFinished());
		assertTrue(state.isSleeping());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testSTZ_DATtrue() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(0L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isStopped());
		assertFalse(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Program counter incorrect", stopAddress, state.getProgramCounter());
	}

	@Test
	public void testSTZ_DATfalse() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(1111L);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.STZ_DAT.value).putInt(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertFalse(state.isStopped());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testFIN_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.FIN_IMD.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.isStopped());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testSTP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int stopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.NOP.value);
		codeByteBuffer.put(OpCode.STP_IMD.value);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isStopped());
		assertFalse(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Program counter incorrect", stopAddress, state.getProgramCounter());
	}

	@Test
	public void testSLP_IMD() throws ExecutionException {
		codeByteBuffer.put(OpCode.SLP_IMD.value);
		int postSleepAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);
		int postFinishAddress = codeByteBuffer.position();

		execute(true);

		assertTrue(state.isSleeping());
		assertFalse(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Program counter incorrect", postSleepAddress, state.getProgramCounter());

		execute(true);

		assertFalse(state.isSleeping());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Program counter incorrect", postFinishAddress, state.getProgramCounter());
	}

	@Test
	public void testERR_ADR() throws ExecutionException {
		// Note: non-fatal error because error handler IS set

		int errorAddr = 0x29;

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).putLong(12345L);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(0L);
		codeByteBuffer.put(OpCode.DIV_DAT.value).putInt(0).putInt(1); // divide by zero
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// errorAddr:
		assertEquals(errorAddr, codeByteBuffer.position());
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Error flag not set", 1L, getData(2));
	}

	@Test
	public void testSLP_VAL() throws ExecutionException {
		int blockCount = 5;

		int startingBlockHeight = api.getCurrentBlockHeight();
		int targetBlockHeight = startingBlockHeight + blockCount;

		codeByteBuffer.put(OpCode.SLP_VAL.value).putLong(blockCount);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// one round only, to check AT has started sleeping
		execute(true);

		assertTrue(state.isSleeping());
		assertFalse(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Sleep-until block height incorrect", targetBlockHeight, state.getSleepUntilHeight().intValue());

		// continue sleeping until AT awakens
		execute(false);

		assertEquals("Reawaken block height incorrect", targetBlockHeight, state.getCurrentBlockHeight());

		assertFalse(state.isSleeping());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Sleep-until block height incorrect", null, state.getSleepUntilHeight());
	}

	@Test
	public void testPCS() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertEquals(expectedStopAddress, state.getOnStopAddress());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

	@Test
	public void testPCS2() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000011111111"));
		codeByteBuffer.put(OpCode.SET_PCS.value);
		int expectedStopAddress = codeByteBuffer.position();
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(0).put(hexToBytes("0000000022222222"));
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertEquals(expectedStopAddress, state.getOnStopAddress());
		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
	}

}
