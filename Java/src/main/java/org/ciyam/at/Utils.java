package org.ciyam.at;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

interface Utils {

	/**
	 * Returns immediate function code enum from code bytes at current position.
	 * <p>
	 * Initial position is <code>codeByteBuffer.position()</code> but on return is incremented by 2.
	 * 
	 * @param codeByteBuffer
	 * @return FunctionCode
	 * @throws CodeSegmentException if we ran out of bytes trying to fetch raw function code value
	 * @throws IllegalFunctionCodeException if we can't convert raw function code to FunctionCode object
	 */
	static FunctionCode getFunctionCode(ByteBuffer codeByteBuffer) throws CodeSegmentException, IllegalFunctionCodeException {
		try {
			final int rawFunctionCode = codeByteBuffer.getShort();

			final FunctionCode functionCode = FunctionCode.valueOf(rawFunctionCode);

			if (functionCode == null)
				throw new IllegalFunctionCodeException("Unknown function code");

			return functionCode;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get function code", e);
		}
	}

	/**
	 * Returns code address from code bytes at current position.
	 * <p>
	 * Initial position is <code>codeByteBuffer.position()</code> but on return is incremented by 4.
	 * <p>
	 * <b>Note:</b> address is not scaled by <code>Constants.VALUE_SIZE</code> unlike other methods in this class.
	 * 
	 * @param codeByteBuffer
	 * @return int address into code segment
	 * @throws CodeSegmentException if we ran out of bytes trying to fetch code address
	 * @throws InvalidAddressException if fetched address points outside of code segment
	 */
	static int getCodeAddress(ByteBuffer codeByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			final int address = codeByteBuffer.getInt();

			if (address < 0 || address > MachineState.MAX_CODE_ADDRESS || address >= codeByteBuffer.limit())
				throw new InvalidAddressException("Code address out of bounds");

			return address;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get code address", e);
		}
	}

	/**
	 * Returns data address from code bytes at current position.
	 * <p>
	 * Initial position is <code>codeByteBuffer.position()</code> but on return is incremented by 4.
	 * <p>
	 * <b>Note:</b> address is returned scaled by <code>Constants.VALUE_SIZE</code>.
	 * 
	 * @param codeByteBuffer
	 * @return int address into data segment
	 * @throws CodeSegmentException if we ran out of bytes trying to fetch data address
	 * @throws InvalidAddressException if fetched address points outside of data segment
	 */
	static int getDataAddress(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			final int address = codeByteBuffer.getInt() * MachineState.VALUE_SIZE;

			if (address < 0 || address + MachineState.VALUE_SIZE > dataByteBuffer.limit())
				throw new InvalidAddressException("Data address out of bounds");

			return address;
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get data address", e);
		}
	}

	/**
	 * Returns byte offset from code bytes at current position.
	 * <p>
	 * Initial position is <code>codeByteBuffer.position()</code> but on return is incremented by 1.
	 * <p>
	 * <b>Note:</b> offset is not scaled by <code>Constants.VALUE_SIZE</code> unlike other methods in this class.
	 * 
	 * @param codeByteBuffer
	 * @return byte offset
	 * @throws CodeSegmentException if we ran out of bytes trying to fetch offset
	 */
	static byte getCodeOffset(ByteBuffer codeByteBuffer) throws CodeSegmentException, InvalidAddressException {
		try {
			// We can't do bounds checking here as we don't have access to program counter.
			return codeByteBuffer.get();
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get code offset", e);
		}
	}

	/**
	 * Returns long immediate value from code bytes at current position.
	 * <p>
	 * Initial position is <code>codeByteBuffer.position()</code> but on return is incremented by 8.
	 * 
	 * @param codeByteBuffer
	 * @return long value
	 * @throws CodeSegmentException if we ran out of bytes trying to fetch value
	 */
	static long getCodeValue(ByteBuffer codeByteBuffer) throws CodeSegmentException {
		try {
			return codeByteBuffer.getLong();
		} catch (BufferUnderflowException e) {
			throw new CodeSegmentException("No code bytes left to get immediate value", e);
		}
	}

}
