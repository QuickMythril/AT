package org.ciyam.at;

import java.nio.ByteBuffer;

enum OpCodeParam {

	/**
	 * Literal <b>64-bit long</b> value supplied from <b>code</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_VAL DEST_ADDR <b>VALUE</b></code><br>
	 *     Example: <code>SET_VAL 3 <b>12345</b></code><br>
	 *     Data segment address 3 will be set to the value 12345.
	 * </p>
	 */
	VALUE(OpCodeParam::compileLong) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Long.valueOf(Utils.getCodeValue(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("#%016x", (Long) value);
		}
	},
	/**
	 * Destination address in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_VAL <b>DEST_ADDR</b> VALUE</code><br>
	 *     Example: <code>SET_VAL <b>3</b> 12345</code><br>
	 *     Data segment address 3 will be set to the value 12345.
	 * </p>
	 */
	DEST_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Indirect destination address in <b>data</b> segment,<br>
	 * using value extracted from supplied address, also in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>IND_DAT <b>INDIRECT_DEST_ADDR</b> SRC_ADDR</code><br>
	 *     Example: <code>IND_DAT <b>3</b> 4</code><br>
	 *     If data segment address 3 contains the value 7,<br>
	 *     and data segment address 4 contains the value 12345,<br>
	 *     then data segment address 7 will be set to the value 12345.
	 * </p>
	 */
	INDIRECT_DEST_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Indirect destination address in <b>data</b> segment,<br>
	 * using value extracted from supplied address, also in <b>data</b> segment,<br>
	 * and then offset by the value extracted from 2nd 'index' address, also in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>IDX_DAT <b>INDIRECT_DEST_ADDR_WITH_INDEX</b> INDEX SRC_ADDR</code><br>
	 *     Example: <code>IDX_DAT <b>3</b> 4 20</code><br>
	 *     If data segment address 3 contains the value 7,<br>
	 *     and data segment address 4 contains the value 2,<br>
	 *     and data segment address 20 contains the value 12345,<br>
	 *     then data segment address 9 (7 + 2) will be set to the value 12345.
	 * </p>
	 * @see OpCodeParam#INDEX
	 */
	INDIRECT_DEST_ADDR_WITH_INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("@($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Source address in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_DAT DEST_ADDR <b>SRC_ADDR</b></code><br>
	 *     Example: <code>SET_DAT 2 <b>3</b></code><br>
	 *     If data segment address 3 contains the value 12345,<br>
	 *     then data segment address 2 will be set to the value 12345.
	 * </p>
	 */
	SRC_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Indirect source address in <b>data</b> segment,<br>
	 * using value extracted from supplied address, also in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_IND DEST_ADDR <b>INDIRECT_SRC_ADDR</b></code><br>
	 *     Example: <code>SET_IND 3 <b>4</b></code><br>
	 *     If data segment address 4 contains the value 7,<br>
	 *     and data segment address 7 contains the value 12345,<br>
	 *     then data segment address 3 will be set to the value 12345.
	 * </p>
	 */
	INDIRECT_SRC_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Indirect source address in <b>data</b> segment,<br>
	 * using value extracted from supplied address, also in <b>data</b> segment,<br>
	 * and then offset by the value extracted from 2nd 'index' address, also in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_IDX DEST_ADDR <b>INDIRECT_SRC_ADDR</b> INDEX</code><br>
	 *     Example: <code>SET_IDX 3 <b>4</b> 5</code><br>
	 *     If data segment address 4 contains the value 7,<br>
	 *     and data segment address 5 contains the value 2,<br>
	 *     and data segment address 9 (7 + 2) contains the value 12345,<br>
	 *     then data segment address 3 will be set to the value 12345.
	 * </p>
	 */
	INDIRECT_SRC_ADDR_WITH_INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("$($%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Offset value extracted from address in <b>data</b> segment.<br>
	 * Used with {@link OpCodeParam#INDIRECT_DEST_ADDR_WITH_INDEX} and {@link OpCodeParam#INDIRECT_DEST_ADDR_WITH_INDEX}
	 * <p></p>
	 * <p>
	 *     Example: <code>SET_IDX DEST_ADDR INDIRECT_SRC_ADDR <b>INDEX</b></code><br>
	 *     Example: <code>SET_IDX 3 4 <b>5</b></code><br>
	 *     If data segment address 4 contains the value 7,<br>
	 *     and data segment address 5 contains the value 2,<br>
	 *     and data segment address 9 (7 + 2) contains the value 12345,<br>
	 *     then data segment address 3 will be set to the value 12345.
	 * </p>
	 * @see OpCodeParam#INDIRECT_DEST_ADDR_WITH_INDEX
	 * @see OpCodeParam#INDIRECT_SRC_ADDR_WITH_INDEX
	 */
	INDEX(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("+ $%08x)", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	},
	/**
	 * Literal program address in <b>code</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>JMP_ADR <b>CODE_ADDR</b></code><br>
	 *     Example: <code>JMP_ADR <b>123</b></code><br>
	 *     Jump (set PC) to code address 123.
	 * </p>
	 */
	CODE_ADDR(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getCodeAddress(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("[%04x]", (Integer) value);
		}
	},
	/**
	 * <b>Byte</b> offset from current program counter, in <b>code</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>BZR_DAT SRC_ADDR <b>OFFSET</b></code><br>
	 *     Example: <code>BZR_DAT 4 <b>123</b></code><br>
	 *     If data segment address 4 contains the value 0,<br>
	 *     then add 123 to program counter (PC).
	 * </p>
	 * <p></p>
	 * <p>
	 *     Note: <code>PC</code> is considered to be immediately before opcode byte.<br>
	 *     Because this value is only a signed byte, maximum offsets are -128 and +127!
	 * </p>
	 */
	OFFSET(OpCodeParam::compileByte) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Byte.valueOf(Utils.getCodeOffset(codeByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("PC+%02x=[%04x]", (int) ((Byte) value), postOpcodeProgramCounter - 1 + (Byte) value);
		}
	},
	/**
	 * Literal <b>16-bit short</b> function code supplied from <b>code</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>EXT_FUN <b>FUNC</b></code><br>
	 *     Example: <code>EXT_FUN <b>0x0001</b></code><br>
	 *     Calls function 0x0001 (ECHO).
	 * </p>
	 * @see FunctionCode#ECHO
	 * @see FunctionCode
	 */
	FUNC(OpCodeParam::compileFunc) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Short.valueOf(codeByteBuffer.getShort());
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			FunctionCode functionCode = FunctionCode.valueOf((Short) value);

			// generic/unknown form
			if (functionCode == null)
				return String.format("FN(%04x)", (Short) value);

			// API pass-through
			if (functionCode == FunctionCode.API_PASSTHROUGH)
				return String.format("API-FN(%04x)", (Short) value);

			return "\"" + functionCode.name() + "\"" + String.format("{%04x}", (Short) value);
		}
	},
	/**
	 * Block height extracted via address in <b>data</b> segment.
	 * <p></p>
	 * <p>
	 *     Example: <code>SLP_DAT <b>BLOCK_HEIGHT</b></code><br>
	 *     Example: <code>SLP_DAT <b>3</b></code><br>
	 *     If data segment address 3 contains the value 12345,<br>
	 *     then the AT will sleep until block height reaches 12345.
	 * </p>
	 */
	BLOCK_HEIGHT(OpCodeParam::compileInt) {
		@Override
		public Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException {
			return Integer.valueOf(Utils.getDataAddress(codeByteBuffer, dataByteBuffer));
		}

		@Override
		protected String toString(Object value, int postOpcodeProgramCounter) {
			return String.format("height $%08x", ((Integer) value) / MachineState.VALUE_SIZE);
		}
	};

	@FunctionalInterface
	private interface Compiler {
		byte[] compile(OpCode opCode, Object arg);
	}
	private final Compiler compiler;

	private OpCodeParam(Compiler compiler) {
		this.compiler = compiler;
	}

	public abstract Object fetch(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer) throws ExecutionException;

	private static byte[] compileByte(OpCode opcode, Object arg) {
		// Highly likely to be an Integer, so try that first
		try {
			int intValue = (int) arg;
			if (intValue < Byte.MIN_VALUE || intValue > Byte.MAX_VALUE)
				throw new ClassCastException("Value too large to compile to byte");

			return new byte[] { (byte) intValue };
		} catch (ClassCastException e) {
			// Try again using Byte
			return new byte[] { (byte) arg };
		}
	}

	private static byte[] compileShort(OpCode opcode, Object arg) {
		short s = (short) arg;
		return new byte[] { (byte) (s >>> 8), (byte) (s) };
	}

	private static byte[] compileInt(OpCode opcode, Object arg) {
		return MachineState.toByteArray((int) arg);
	}

	private static byte[] compileLong(OpCode opcode, Object arg) {
		// Highly likely to be a Long, so try that first
		try {
			return MachineState.toByteArray((long) arg);
		} catch (ClassCastException e) {
			// Try again using Integer
			return MachineState.toByteArray((long)(int) arg);
		}
	}

	private static byte[] compileFunc(OpCode opcode, Object arg) {
		try {
			FunctionCode func = (FunctionCode) arg;
			opcode.preExecuteCheck(func.value);
			return compileShort(opcode, func.value);
		} catch (ClassCastException e) {
			// Couldn't cast to FunctionCode,
			// but try Short in case caller is using API-PASSTHROUGH range
			return compileShort(opcode, arg);
		} catch (ExecutionException e) {
			// Wrong opcode for this function
			throw new ClassCastException("Wrong opcode for this function");
		}
	}

	protected byte[] compile(OpCode opcode, Object arg) {
		return this.compiler.compile(opcode, arg);
	}

	public String disassemble(ByteBuffer codeByteBuffer, ByteBuffer dataByteBuffer, int postOpcodeProgramCounter) throws ExecutionException {
		Object value = fetch(codeByteBuffer, dataByteBuffer);

		return this.toString(value, postOpcodeProgramCounter);
	}

	protected abstract String toString(Object value, int postOpcodeProgramCounter);

}
