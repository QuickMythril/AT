package org.ciyam.at;

import static org.junit.Assert.*;

import org.ciyam.at.test.ExecutableTest;
import org.junit.Test;

public class ValueOpCodeTests extends ExecutableTest {

	@Test
	public void testSET_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 2222L, getData(2));
	}

	/** Check that trying to use an address outside data segment throws a fatal error. */
	@Test
	public void testSET_VALunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(9999).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertTrue(state.hadFatalError());
	}

	@Test
	public void testADD_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.ADD_VAL.value).putInt(2).putLong(3333L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 2222L + 3333L, getData(2));
	}

	/** Check that trying to use an address outside data segment throws a fatal error. */
	@Test
	public void testADD_VALunbounded() throws ExecutionException {
		codeByteBuffer.put(OpCode.ADD_VAL.value).putInt(9999).putLong(3333L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertTrue(state.hadFatalError());
	}

	/** Check that adding to an unsigned long value overflows correctly. */
	@Test
	public void testADD_VALoverflow() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(0x7fffffffffffffffL);
		codeByteBuffer.put(OpCode.ADD_VAL.value).putInt(2).putLong(0x8000000000000099L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 0x0000000000000098L, getData(2));
	}

	@Test
	public void testSUB_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.SUB_VAL.value).putInt(3).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 3333L - 2222L, getData(3));
	}

	@Test
	public void testMUL_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(3333L);
		codeByteBuffer.put(OpCode.MUL_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", (3333L * 2222L), getData(2));
	}

	@Test
	public void testDIV_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", (3333L / 2222L), getData(2));
	}

	/** Check divide-by-zero throws fatal error because error handler not set. */
	@Test
	public void testDIV_VALzero() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_VAL.value).putInt(3).putLong(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertTrue(state.hadFatalError());
	}

	/** Check divide-by-zero is non-fatal because error handler is set. */
	@Test
	public void testDIV_DATzeroWithOnError() throws ExecutionException {
		int errorAddr = 0x20; // adjust this manually

		codeByteBuffer.put(OpCode.ERR_ADR.value).putInt(errorAddr);

		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(3).putLong(3333L);
		codeByteBuffer.put(OpCode.DIV_VAL.value).putInt(3).putLong(0);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		// errorAddr:
		assertEquals(errorAddr, codeByteBuffer.position());
		// Set 1 at address 1 to indicate we handled error OK
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(1).putLong(1L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Error flag not set", 1L, getData(1));
	}

	@Test
	public void testSHL_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SHL_VAL.value).putInt(2).putLong(3L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 2222L << 3, getData(2));
	}

	@Test
	public void testSHL_VALexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SHL_VAL.value).putInt(2).putLong(3333L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 0L, getData(2));
	}

	@Test
	public void testSHR_VAL() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SHR_VAL.value).putInt(2).putLong(3L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 2222L >>> 3, getData(2));
	}

	@Test
	public void testSHR_VALexcess() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(2222L);
		codeByteBuffer.put(OpCode.SHR_VAL.value).putInt(2).putLong(3333L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", 0L, getData(2));
	}

	@Test
	public void testSHR_VALsign() throws ExecutionException {
		codeByteBuffer.put(OpCode.SET_VAL.value).putInt(2).putLong(-1L);
		codeByteBuffer.put(OpCode.SHR_VAL.value).putInt(2).putLong(3L);
		codeByteBuffer.put(OpCode.FIN_IMD.value);

		execute(true);

		assertTrue(state.isFinished());
		assertFalse(state.hadFatalError());
		assertEquals("Data does not match", -1L >>> 3, getData(2));
		assertTrue("Sign does not match", getData(2) >= 0);
	}

}
