package pl.edu.uj.synchrotron.jive;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevEncoded;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.DevVarDoubleStringArray;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoDs.TangoConst;

/**
 * Created by lukasz on 27.04.15.
 * This file is element of RESTful Jive application project.
 * You are free to use, copy and edit whole application or any of its components.
 * Application comes with no warranty. Altough author is trying to make it best, it may work or it may not work.
 */
public class TangoDataProcessing implements TangoConst {
/**
 * Extract data read from device and convert to String.
 *
 * @param data Data read from device
 * @param ai   Parameter of read data.
 * @return String with data.
 */
static String extractDataValue(DeviceAttribute data, AttributeInfo ai) {
	StringBuffer ret_string = new StringBuffer();
	try {
		// Add dimension of the attribute but only if having a meaning
		boolean printIndex = true;
		boolean checkLimit = true;
		// Add values
		switch (ai.data_type) {
			case Tango_DEV_STATE: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					ret_string.append(data.extractState());
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_UCHAR: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					short dummy = data.extractUChar();
					ret_string.append(Short.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_SHORT: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					short dummy = data.extractShort();
					ret_string.append(Short.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_BOOLEAN: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					boolean dummy = data.extractBoolean();
					ret_string.append(Boolean.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_USHORT: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					int dummy = data.extractUShort();
					ret_string.append(Integer.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_LONG: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					int dummy = data.extractLong();
					ret_string.append(Integer.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_ULONG: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					long dummy = data.extractULong();
					ret_string.append(Long.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_LONG64: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					long dummy = data.extractLong64();
					ret_string.append(Long.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_ULONG64: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					long dummy = data.extractULong64();
					ret_string.append(Long.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_DOUBLE: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					double dummy = data.extractDouble();
					ret_string.append(Double.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_FLOAT: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					float dummy = data.extractFloat();
					ret_string.append(Float.toString(dummy));
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_STRING: {
				if (ai.data_format.value() == AttrDataFormat._SCALAR) {
					String dummy = data.extractString();
					ret_string.append(dummy);
				} else {
					ret_string.append("Too many values!");
				}
			}
			break;
			case Tango_DEV_ENCODED: {
				printIndex = true;
				DevEncoded e = data.extractDevEncoded();
				ret_string.append("Format: " + e.encoded_format + "\n");
				int nbRead = e.encoded_data.length;
				int start = getLimitMin(checkLimit, ret_string, nbRead);
				int end = getLimitMax(checkLimit, ret_string, nbRead, false);
				for (int i = start; i < end; i++) {
					short vs = (short) e.encoded_data[i];
					vs = (short) (vs & 0xFF);
					printArrayItem(ret_string, i, printIndex, Short.toString(vs), false);
				}
			}
			break;
			default:
				ret_string.append("Unsupported attribute type code=" + ai.data_type + "\n");
				break;
		}
	} catch (DevFailed e) {
		// ErrorPane.showErrorMessage(this,device.name() + "/" + ai.name,e);
	}
	return ret_string.toString();
}

/**
 * Check maximum length of response.
 *
 * @param checkLimit
 * @param retStr     Response string.
 * @param length     Length of current response.
 * @param writable   Defines if value is writable.
 * @return Maximum response length.
 */
private static int getLimitMax(boolean checkLimit, StringBuffer retStr, int length, boolean writable) {
	if (length < 100) {
		return length;
	}
	return 100;
}

/**
 * Check minimum length of response.
 *
 * @param checkLimit
 * @param retStr     Response string.
 * @param length     Length of current response.
 * @return Minimum response length.
 */
private static int getLimitMin(boolean checkLimit, StringBuffer retStr, int length) {
	return 0;
}

/**
 * Parses string to be printed
 *
 * @param str       String to be printed.
 * @param idx       Number of value in array.
 * @param printIdx  Defines if array has more than one value.
 * @param value     Value that was read/written.
 * @param writeable Defines if value is writable.
 */
private static void printArrayItem(StringBuffer str, int idx, boolean printIdx, String value, boolean writeable) {
	if (!writeable) {
		if (printIdx)
			str.append("Read [" + idx + "]\t" + value + "\n");
		else
			str.append("Read:\t" + value + "\n");
	} else {
		if (printIdx)
			str.append("Set [" + idx + "]\t" + value + "\n");
		else
			str.append("Set:\t" + value + "\n");
	}
}

/**
 * Check if attribute can be written with new value.
 *
 * @param ai Attribute to be checked.
 * @return True when attribute can be written.
 */
static boolean isWritable(AttributeInfo ai) {
	return (ai.writable.value() == AttrWriteType._READ_WITH_WRITE)
			|| (ai.writable.value() == AttrWriteType._READ_WRITE) || (ai.writable.value() == AttrWriteType._WRITE);
}

/**
 * Check if data can be plotted.
 *
 * @param outType Identifier of data type.
 * @return True if plotable.
 */
static boolean isPlotable(int outType) {
	switch (outType) {
		case Tango_DEVVAR_CHARARRAY:
		case Tango_DEVVAR_USHORTARRAY:
		case Tango_DEVVAR_SHORTARRAY:
		case Tango_DEVVAR_ULONGARRAY:
		case Tango_DEVVAR_LONGARRAY:
		case Tango_DEVVAR_FLOATARRAY:
		case Tango_DEVVAR_DOUBLEARRAY:
			return true;
	}
	return false;
}

/**
 * Check if attribute could be plotted.
 *
 * @param ai Attribute to be checked.
 * @return True when attribute can be plotted.
 */
static boolean isPlotable(AttributeInfo ai) {
	if ((ai.data_type == Tango_DEV_STRING) || (ai.data_type == Tango_DEV_STATE)
			|| (ai.data_type == Tango_DEV_BOOLEAN))
		return false;
	return (ai.data_format.value() == AttrDataFormat._SPECTRUM) || (ai.data_format.value() == AttrDataFormat._IMAGE);
}

/**
 * Check whether attribute could be read, written or both.
 *
 * @param ai Attribute to be checked.
 * @return String defining write permission.
 */
static String getWriteString(AttributeInfo ai) {
	switch (ai.writable.value()) {
		case AttrWriteType._READ:
			return "READ";
		case AttrWriteType._READ_WITH_WRITE:
			return "READ_WITH_WRITE";
		case AttrWriteType._READ_WRITE:
			return "READ_WRITE";
		case AttrWriteType._WRITE:
			return "WRITE";
	}
	return "Unknown";
}

/**
 * Check whether attribute could be presented as scalar, spectrum or image.
 *
 * @param ai Attribute to be checked.
 * @return String defining presentation format.
 */
static String getFormatString(AttributeInfo ai) {
	switch (ai.data_format.value()) {
		case AttrDataFormat._SCALAR:
			return "Scalar";
		case AttrDataFormat._SPECTRUM:
			return "Spectrum";
		case AttrDataFormat._IMAGE:
			return "Image";
	}
	return "Unknown";
}

/**
 * Extract data from DeviceAttribute to one dimensional array.
 *
 * @param data DeviceAttribute to extract data from.
 * @param ai   Define data format.
 * @return Array of data that can be plotted.
 */
static double[] extractSpectrumPlotData(DeviceAttribute data, AttributeInfo ai) {
	double[] ret = new double[0];
	int i;
	try {
		int start = getLimitMinForPlot(data.getNbRead());
		int end = getLimitMaxForPlot(data.getNbRead());
		System.out.println("Start: " + start);
		System.out.println("End:   " + end);
		switch (ai.data_type) {
			case Tango_DEV_UCHAR: {
				short[] dummy = data.extractUCharArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_SHORT: {
				short[] dummy = data.extractShortArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_USHORT: {
				int[] dummy = data.extractUShortArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_LONG: {
				int[] dummy = data.extractLongArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_DOUBLE: {
				double[] dummy = data.extractDoubleArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = dummy[i];
			}
			break;
			case Tango_DEV_FLOAT: {
				float[] dummy = data.extractFloatArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_LONG64: {
				long[] dummy = data.extractLong64Array();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_ULONG64: {
				long[] dummy = data.extractULong64Array();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
			case Tango_DEV_ULONG: {
				long[] dummy = data.extractULongArray();
				ret = new double[end - start];
				for (i = start; i < end; i++)
					ret[i - start] = (double) dummy[i];
			}
			break;
		}
	} catch (DevFailed e) {
		// ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name,
		// e);
	}
	return ret;
}

/**
 * Extract data from DeviceAttribute to two dimensional array.
 *
 * @param data DeviceAttribute to extract data from.
 * @param ai   Define data format.
 * @return Array of data that can be plotted.
 */
static double[][] extractImagePlotData(DeviceAttribute data, AttributeInfo ai) {
	double[][] ret = new double[0][0];
	int i, j, k, dimx, dimy;
	try {
		dimx = data.getDimX();
		dimy = data.getDimY();
		switch (ai.data_type) {
			case Tango_DEV_UCHAR: {
				short[] dummy = data.extractUCharArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = (double) dummy[k++];
			}
			break;
			case Tango_DEV_SHORT: {
				short[] dummy = data.extractShortArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = (double) dummy[k++];
			}
			break;
			case Tango_DEV_USHORT: {
				int[] dummy = data.extractUShortArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = (double) dummy[k++];
			}
			break;
			case Tango_DEV_LONG: {
				int[] dummy = data.extractLongArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = (double) dummy[k++];
			}
			break;
			case Tango_DEV_DOUBLE: {
				double[] dummy = data.extractDoubleArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = dummy[k++];
			}
			break;
			case Tango_DEV_FLOAT: {
				float[] dummy = data.extractFloatArray();
				ret = new double[dimy][dimx];
				for (j = 0, k = 0; j < dimy; j++)
					for (i = 0; i < dimx; i++)
						ret[j][i] = (double) dummy[k++];
			}
			break;
		}
	} catch (DevFailed e) {
		// ErrorPane.showErrorMessage(this, device.name() + "/" + ai.name,
		// e);
	}
	return ret;
}

/**
 * Check maximum length of data.
 *
 * @param length Length of current data.
 * @return Maximum length.
 */
static int getLimitMaxForPlot(int length) {
	if (length < 100) {
		return length;
	}
	return 100;
}

/**
 * Check minimum length of data.
 *
 * @param length Length of current data.
 * @return Minimum length.
 */
static int getLimitMinForPlot(int length) {
	return 0;
}

/**
 * Adds value to DeviceAttribute.
 *
 * @param argin Value to be added.
 * @param send  Value will be added to this DeviceAttribute.
 * @param ai    Define data format.
 * @return DeviceAttribute with new value.
 * @throws NumberFormatException
 */
static DeviceAttribute insertData(String argin, DeviceAttribute send, AttributeInfo ai)
		throws NumberFormatException {
	ArgParser arg = new ArgParser(argin);
	switch (ai.data_type) {
		case Tango_DEV_UCHAR:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert_uc(arg.parse_uchar());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert_uc(arg.parse_uchar_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert_uc(arg.parse_uchar_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_BOOLEAN:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_boolean());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_boolean_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_boolean_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_SHORT:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_short());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_short_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_short_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_USHORT:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert_us(arg.parse_ushort());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert_us(arg.parse_ushort_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert_us(arg.parse_ushort_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_LONG:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_long());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_long_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_long_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_ULONG:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert_ul(arg.parse_ulong());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert_ul(arg.parse_ulong_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert_ul(arg.parse_ulong_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_LONG64:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_long64());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_long64_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_ULONG64:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert_u64(arg.parse_long64());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert_u64(arg.parse_long64_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert_u64(arg.parse_long64_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_FLOAT:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_float());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_float_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_float_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_DOUBLE:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_double());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_double_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_double_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		case Tango_DEV_STRING:
			switch (ai.data_format.value()) {
				case AttrDataFormat._SCALAR:
					send.insert(arg.parse_string());
					break;
				case AttrDataFormat._SPECTRUM:
					send.insert(arg.parse_string_array());
					break;
				case AttrDataFormat._IMAGE:
					send.insert(arg.parse_string_image(), arg.get_image_width(), arg.get_image_height());
					break;
			}
			break;
		default:
			throw new NumberFormatException("Attribute type not supported code=" + ai.data_type);
	}
	return send;
}

/**
 * Adds value to DeviceData.
 *
 * @param argin   Value to be added.
 * @param send    Value will be added to this DeviceData.
 * @param outType Identifier of data type.
 * @return DeviceData with new value.
 * @throws NumberFormatException
 */
static DeviceData insertData(String argin, DeviceData send, int outType) throws NumberFormatException {
	if (outType == Tango_DEV_VOID)
		return send;
	ArgParser arg = new ArgParser(argin);
	switch (outType) {
		case Tango_DEV_BOOLEAN:
			send.insert(arg.parse_boolean());
			break;
		case Tango_DEV_USHORT:
			send.insert_us(arg.parse_ushort());
			break;
		case Tango_DEV_SHORT:
			send.insert(arg.parse_short());
			break;
		case Tango_DEV_ULONG:
			send.insert_ul(arg.parse_ulong());
			break;
		case Tango_DEV_LONG:
			send.insert(arg.parse_long());
			break;
		case Tango_DEV_FLOAT:
			send.insert(arg.parse_float());
			break;
		case Tango_DEV_DOUBLE:
			send.insert(arg.parse_double());
			break;
		case Tango_DEV_STRING:
			send.insert(arg.parse_string());
			break;
		case Tango_DEVVAR_CHARARRAY:
			send.insert(arg.parse_char_array());
			break;
		case Tango_DEVVAR_USHORTARRAY:
			send.insert_us(arg.parse_ushort_array());
			break;
		case Tango_DEVVAR_SHORTARRAY:
			send.insert(arg.parse_short_array());
			break;
		case Tango_DEVVAR_ULONGARRAY:
			send.insert_ul(arg.parse_ulong_array());
			break;
		case Tango_DEVVAR_LONGARRAY:
			send.insert(arg.parse_long_array());
			break;
		case Tango_DEVVAR_FLOATARRAY:
			send.insert(arg.parse_float_array());
			break;
		case Tango_DEVVAR_DOUBLEARRAY:
			send.insert(arg.parse_double_array());
			break;
		case Tango_DEVVAR_STRINGARRAY:
			send.insert(arg.parse_string_array());
			break;
		case Tango_DEVVAR_LONGSTRINGARRAY:
			send.insert(new DevVarLongStringArray(arg.parse_long_array(), arg.parse_string_array()));
			break;
		case Tango_DEVVAR_DOUBLESTRINGARRAY:
			send.insert(new DevVarDoubleStringArray(arg.parse_double_array(), arg.parse_string_array()));
			break;
		case Tango_DEV_STATE:
			send.insert(DevState.from_int(arg.parse_ushort()));
			break;
		default:
			throw new NumberFormatException("Command type not supported code=" + outType);
	}
	return send;
}

/**
 * Extract data read from device and converts to String.
 *
 * @param data    Data read from device
 * @param outType Identifier of data type.
 * @return String with data.
 */
static String extractData(DeviceData data, int outType) {
	StringBuffer ret_string = new StringBuffer();
	switch (outType) {
		case Tango_DEV_VOID:
			break;
		case Tango_DEV_BOOLEAN:
			ret_string.append(Boolean.toString(data.extractBoolean()));
			ret_string.append("\n");
			break;
		case Tango_DEV_USHORT:
			ret_string.append(Integer.toString(data.extractUShort()));
			ret_string.append("\n");
			break;
		case Tango_DEV_SHORT:
			ret_string.append(Short.toString(data.extractShort()));
			ret_string.append("\n");
			break;
		case Tango_DEV_ULONG:
			ret_string.append(Long.toString(data.extractULong()));
			ret_string.append("\n");
			break;
		case Tango_DEV_LONG:
			ret_string.append(Integer.toString(data.extractLong()));
			ret_string.append("\n");
			break;
		case Tango_DEV_FLOAT:
			ret_string.append(Float.toString(data.extractFloat()));
			ret_string.append("\n");
			break;
		case Tango_DEV_DOUBLE:
			ret_string.append(Double.toString(data.extractDouble()));
			ret_string.append("\n");
			break;
		case Tango_DEV_STRING:
			ret_string.append(data.extractString());
			ret_string.append("\n");
			break;
		case Tango_DEVVAR_CHARARRAY: {
			byte[] dummy = data.extractByteArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++) {
				ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]));
				if (dummy[i] >= 32)
					ret_string.append(" '" + (new Character((char) dummy[i]).toString()) + "'");
				else
					ret_string.append(" '.'");
				ret_string.append("\n");
			}
		}
		break;
		case Tango_DEVVAR_USHORTARRAY: {
			int[] dummy = data.extractUShortArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_SHORTARRAY: {
			short[] dummy = data.extractShortArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Short.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_ULONGARRAY: {
			long[] dummy = data.extractULongArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Long.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_LONGARRAY: {
			int[] dummy = data.extractLongArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Integer.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_FLOATARRAY: {
			float[] dummy = data.extractFloatArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Float.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_DOUBLEARRAY: {
			double[] dummy = data.extractDoubleArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t" + Double.toString(dummy[i]) + "\n");
		}
		break;
		case Tango_DEVVAR_STRINGARRAY: {
			String[] dummy = data.extractStringArray();
			int start = getLimitMin(ret_string, dummy.length);
			int end = getLimitMax(ret_string, dummy.length);
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + dummy[i] + "\n");
		}
		break;
		case Tango_DEVVAR_LONGSTRINGARRAY: {
			DevVarLongStringArray dummy = data.extractLongStringArray();
			int start = getLimitMin(ret_string, dummy.lvalue.length);
			int end = getLimitMax(ret_string, dummy.lvalue.length);
			ret_string.append("lvalue:\n");
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Integer.toString(dummy.lvalue[i]) + "\n");
			start = getLimitMin(ret_string, dummy.svalue.length);
			end = getLimitMax(ret_string, dummy.svalue.length);
			ret_string.append("svalue:\n");
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + dummy.svalue[i] + "\n");
		}
		break;
		case Tango_DEVVAR_DOUBLESTRINGARRAY: {
			DevVarDoubleStringArray dummy = data.extractDoubleStringArray();
			int start = getLimitMin(ret_string, dummy.dvalue.length);
			int end = getLimitMax(ret_string, dummy.dvalue.length);
			ret_string.append("dvalue:\n");
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + Double.toString(dummy.dvalue[i]) + "\n");
			start = getLimitMin(ret_string, dummy.svalue.length);
			end = getLimitMax(ret_string, dummy.svalue.length);
			ret_string.append("svalue:\n");
			for (int i = start; i < end; i++)
				ret_string.append("[" + i + "]\t " + dummy.svalue[i] + "\n");
		}
		break;
		case Tango_DEV_STATE:
			ret_string.append(Tango_DevStateName[data.extractDevState().value()]);
			ret_string.append("\n");
			break;
		default:
			ret_string.append("Unsupported command type code=" + outType);
			ret_string.append("\n");
			break;
	}
	return ret_string.toString();
}

/**
 * Check maximum length of response.
 *
 * @param retStr Response string.
 * @param length Length of current response.
 * @return Maximum response length.
 */
static int getLimitMax(StringBuffer retStr, int length) {
	if (length < 100) {
		return length;
	}
	return 100;
}

/**
 * Check minimum length of response.
 *
 * @param retStr Response string.
 * @param length Length of current response.
 */
static int getLimitMin(StringBuffer retStr, int length) {
	// if(length<=common.getAnswerLimitMin()) {
	// retStr.append("Array cannot be displayed. (You may change the AnswerLimitMin)\n");
	// return length;
	// } else {
	// return common.getAnswerLimitMin();
	return 0;
	// }
}

}
