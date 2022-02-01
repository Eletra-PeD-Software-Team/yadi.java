package yadi.dlms.cosem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import yadi.dlms.DlmsException;
import yadi.dlms.DlmsType;
import yadi.dlms.classes.clock.ClockStatus;
import yadi.dlms.classes.clock.CosemDate;
import yadi.dlms.classes.clock.CosemDateTime;
import yadi.dlms.classes.clock.CosemTime;

public class CosemParser {

	private final ByteArrayInputStream is;
	
	static public CosemParser make(byte[] data) {
		return new CosemParser(data);
	}
	
	public CosemParser(byte[] data) {
		is = new ByteArrayInputStream(data);
	}
	
	public int parseSize() {
		int size = is.read();
		
		if (size <= 0x80) {
			return size;
		}
		
		switch (size) {
		case 0x81:
			return is.read();
		case 0x82:
			return readU16();
		case 0x83:
			return (readU16() << 8) | is.read();
		case 0x84:
			return readU32();
		default:
			throw new IllegalArgumentException("invalid size = "+size);
		}
	}
	
	public int parseStructureSize() {
		if (is.read() != DlmsType.STRUCTURE.tag) {
			throw new IllegalArgumentException();
		}
		return parseSize();
	}
	
	public void verifyStructureSize(int size) {
		if (is.read() != DlmsType.STRUCTURE.tag) {
			throw new IllegalArgumentException();
		}
		if (size != parseSize()) {
			throw new IllegalArgumentException();
		}
	}
	
	public int parseArraySize() {
		if (is.read() != DlmsType.ARRAY.tag) {
			throw new IllegalArgumentException();
		}
		return parseSize();
	}
	
	public void verifyArraySize(int size) {
		if (is.read() != DlmsType.ARRAY.tag) {
			throw new IllegalArgumentException();
		}
		if (size != parseSize()) {
			throw new IllegalArgumentException();
		}
	}
	
	public String string() {
		if (is.read() != DlmsType.STRING.tag) {
			throw new IllegalArgumentException();
		}
		int size = parseSize();
		byte[] rawbytes = new byte[size];
		is.read(rawbytes, 0, size);
		return new String(rawbytes);
	}

	public byte[] octetString() {
		int tag = is.read();
		if (tag != DlmsType.OCTET_STRING.tag) {
			throw new IllegalArgumentException("received tag = "+tag);
		}
		int size = parseSize();
		byte[] rawbytes = new byte[size];
		is.read(rawbytes, 0, size);
		return rawbytes;
	}
	
	public byte[] rawString() {
		int tag = is.read();
		int size = parseSize();
		byte[] rawbytes = new byte[size];
		is.read(rawbytes, 0, size);
		return rawbytes;
	}
	
	public byte[] bcd() {
		int tag = is.read();
		if (tag != DlmsType.BCD.tag) {
			throw new IllegalArgumentException("received tag = "+tag);
		}
		int size = parseSize();
		byte[] rawbytes = new byte[size];
		is.read(rawbytes, 0, size);
		return rawbytes;
	}
	
	public boolean[] bitstring() {
		int tag = is.read();
		if (tag != DlmsType.BITSTRING.tag) {
			throw new IllegalArgumentException("invalid bitstring tag = "+tag);
		}
		
		int size= parseSize();
		boolean[] result = new boolean[size];
		
		int bit = 0x80;
		int value = is.read();
		
		for (int i = 0; i < size; ++i) {
			if (bit == 0) {
				value = is.read();
				bit = 0x80;
			}
			result[i] = (value & bit) == bit;
			bit >>>= 1;
		}
		
		return result;
	}
	
	public boolean bool() {
		if (is.read() != DlmsType.BOOLEAN.tag) {
			throw new IllegalArgumentException();
		}
		if (is.read() == 0) {
			return false;
		}
		return true;
	}
	
	public CosemDate date() {
		if (is.read() != DlmsType.OCTET_STRING.tag || is.available() < 7) {
			throw new IllegalArgumentException();
		}
		if (is.read() != 5) {
			throw new IllegalArgumentException();
		}
		return new CosemDate(readU16(), is.read(), is.read(), is.read());
	}
	
	public CosemTime time() {
		if (is.read() != DlmsType.OCTET_STRING.tag || is.available() < 5) {
			throw new IllegalArgumentException();
		}
		if (is.read() != 4) {
			throw new IllegalArgumentException();
		}
		return new CosemTime(is.read(), is.read(), is.read(), is.read());
	}
	
	public CosemDateTime datetime() {
		if (is.read() != DlmsType.OCTET_STRING.tag || is.available() < 13) {
			throw new IllegalArgumentException();
		}
		if (readU8() != 12) {
			throw new IllegalArgumentException();
		}
		CosemDate date = new CosemDate(readU16(), is.read(), is.read(), is.read());
		CosemTime time = new CosemTime(is.read(), is.read(), is.read(), is.read());
		int deviation = readU16();
		ClockStatus status = new ClockStatus(is.read());
		return new CosemDateTime(date, time, deviation, status);
	}
	
	public float float32() {
		if (is.read() != DlmsType.FLOAT32.tag || is.available() < 4) {
			throw new IllegalArgumentException();
		}
		return Float.intBitsToFloat(Integer.reverseBytes(readU32()));
	}
	
	public double float64() {
		if (is.read() != DlmsType.FLOAT64.tag || is.available() < 8) {
			throw new IllegalArgumentException();
		}
		return Double.longBitsToDouble(Long.reverseBytes(readU64()));
	}
	
	public int enumeration() {
		if (is.read() != DlmsType.ENUM.tag || is.available() < 1) {
			throw new IllegalArgumentException();
		}
		return is.read();
	}

	public int int8() {
		if (is.read() != DlmsType.INT8.tag || is.available() < 1) {
			throw new IllegalArgumentException();
		}
		byte val = (byte)is.read();
		return (int)val;
	}
	
	public int uint8() {
		if (is.read() != DlmsType.UINT8.tag || is.available() < 1) {
			throw new IllegalArgumentException();
		}
		return is.read() & 0xFF;
	}
	
	public int int16() {
		if (is.read() != DlmsType.INT16.tag || is.available() < 2) {
			throw new IllegalArgumentException();
		}
		return readI16();
	}
	
	public int uint16() {
		if (is.read() != DlmsType.UINT16.tag || is.available() < 2) {
			throw new IllegalArgumentException();
		}
		return readU16();
	}
	
	public int int32() {
		if (is.read() != DlmsType.INT32.tag || is.available() < 4) {
			throw new IllegalArgumentException();
		}
		return readI32();
	}
	
	public int uint32() {
		if (is.read() != DlmsType.UINT32.tag || is.available() < 4) {
			throw new IllegalArgumentException();
		}
		return readU32();
	}
	
	public long int64() {
		if (is.read() != DlmsType.INT64.tag || is.available() < 8) {
			throw new IllegalArgumentException();
		}
		return readI64();
	}
	
	public long uint64() {
		if (is.read() != DlmsType.UINT64.tag || is.available() < 8) {
			throw new IllegalArgumentException();
		}
		return readU64();
	}
	
	public int integer() {
		int tag = is.read();
		
		if (tag == DlmsType.UINT8.tag) {
			return readU8();
		}
		
		if (tag == DlmsType.INT8.tag) {
			return readI8();
		}
		
		if (tag == DlmsType.UINT16.tag) {
			return readU16();
		}
		
		if (tag == DlmsType.INT16.tag) {
			return readI16();
		}
		
		if (tag == DlmsType.UINT32.tag) {
			return readU32();
		}
		
		if (tag == DlmsType.INT32.tag) {
			return readI32();
		}
		
		if (tag == DlmsType.ENUM.tag) {
			return readU8();
		}
		
		if (tag == DlmsType.OCTET_STRING.tag) {
			int size = parseSize();
			int val = 0;
			
			for (int i = 0; i < size; ++i) {
				val <<= 8;
				val |= readU8();
			}
			
			return val;
		}
		
		System.out.println("tag == "+tag);
		
		throw new IllegalArgumentException();
	}
	
	private int readI8() {
		return is.read();
	}
	
	private int readU8() {
		return is.read() & 0xFF;
	}
	
	private int readI16() {
		return ((is.read() << 8) | is.read());
	}
	
	private int readU16() {
		return readI16() & 0xFFFF;
	}
	
	private int readI32() {
		return ((readI16() << 16) | readI16());
	}
	
	private int readU32() {
		return readI32() & 0xFFFFFFFF;
	}
	
	private long readI64() {
		return ((readI32() << 32) | readI32());
	}
	
	private long readU64() {
		return readI64() & 0xFFFFFFFFFFFFFFFFL;
	}
	
	public DlmsType getNextType() throws DlmsException {
		is.mark(10);
		DlmsType tag = DlmsType.fromTag(is.read());
		
		if (tag == DlmsType.OCTET_STRING) {
			if (this.parseSize() == 12) {
				is.reset();
				return DlmsType.OCTET_STRING_12;
			}
		}
		
		is.reset();
		return tag;
	}

	public byte[] getNextItemRawData() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			DlmsType type = DlmsType.fromTag(is.read());
			os.write(type.tag);
			switch (type) {
			case ARRAY:
				break;
			case BCD:
				break;
			case BITSTRING:
				break;
			case BOOLEAN:
				break;
			case DATE:
				break;
			case DATE_TIME:
				break;
			case ENUM:
				break;
			case FLOAT32:
				break;
			case FLOAT64:
				break;
			case INT16:
				break;
			case INT32:
				break;
			case INT64:
				break;
			case INT8:
				break;
			case OCTET_STRING:
				break;
			case OCTET_STRING_AS_DATETIME:
				break;
			case STRING:
				break;
			case STRUCTURE:
				break;
			case TIME:
				break;
			case UINT16:
				break;
			case UINT32:
				break;
			case UINT64:
				break;
			case UINT8:
				break;
			case UTF8_STRING:
				break;
			}
		} catch (Exception e) {
			//
		}
		return os.toByteArray();
	}

	public boolean hasRemaining() {
		return is.available() > 0;
	}
	
}
