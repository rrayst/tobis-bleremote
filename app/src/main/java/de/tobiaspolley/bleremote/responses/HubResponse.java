package de.tobiaspolley.bleremote.responses;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import de.tobiaspolley.bleremote.structs.MappingOptions;
import de.tobiaspolley.bleremote.structs.ModeInformationType;

public abstract class HubResponse {

    private String text;

    protected HubResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static HubResponse decodeMessage(byte[] data) {
        StringBuilder decoded = new StringBuilder();
        if (data == null || data.length == 0)
            return new UnhandledResponse(decoded.toString());

        if (data[0] != data.length)
            decoded.append("unusual length (byte 0) not matching length, ");

        byte length = data[0];

        if (data.length > 1 && data[1] != 0)
            decoded.append("unusual hub indicator, ");

        if (data.length <= 2)
            return new UnhandledResponse(decoded.toString());

        byte messageType = data[2];
        decoded.append(messageTypeToString(messageType));

        switch (messageType) {
            case 0x01:
                decoded.append("property=" + propertyToString(data[3]) + ", ");
                switch (data[3]) {
                    case 0x03:
                        decoded.append("fwVersion=" + versionToString(fromByteArray(data, 4)));
                        // last byte is 0x10 - unused ??
                        break;
                    case 0x0A:
                        decoded.append("version=" + data[4] + "." + data[5]);
                        break;
                }
                break;
            case 0x04:
                byte port = data[3];
                decoded.append("port id=" + portIdToString(port) + ", ");

                byte event = data[4];
                switch (event) {
                    case 0x00:
                        decoded.append("Detached I/O, ");
                        break;
                    case 0x01:
                        decoded.append("Attached I/O, ");
                        break;
                    case 0x02:
                        decoded.append("Attached Virtual I/O, ");
                        break;
                    default:
                        decoded.append("unknown I/O event");
                        break;
                }

                int pos = 5;
                int ioType = -1;

                if (data[4] == 0x01 || data[4] == 0x02) {
                    ioType = data[pos+1] & 0xFF;
                    ioType = (ioType << 8) | (data[pos] & 0xFF);
                    pos += 2;

                    decoded.append("io type=" + IOTypeIdToString(ioType) + ", ");
                }

                if (data[4] == 0x01) {
                    int hardwareVersion = fromByteArray(data, pos);
                    pos += 4;
                    int softwareVersion = fromByteArray(data, pos);
                    pos += 4;

                    decoded.append("hardwareVersion=" + versionToString(hardwareVersion) + ", softwareVersion=" + versionToString(softwareVersion) + ",");
                }

                if (data[4] == 0x02) {
                    decoded.append("portA=" + portIdToString(data[pos]) + ", portB=" + portIdToString(data[pos+1]) + ", ");
                }

                if (event == 0x00)
                    return new PortDisconnectedResponse(decoded.toString(), port);
                if (event == 0x01)
                    return new PortConnectedResponse(decoded.toString(), port, ioType);

                break;
            case 0x05:
                decoded.append("error introduced command '"+messageTypeToString(data[3])+"',");
                decoded.append("error: " + errorCodeToString(data[4]));
                break;
            case 0x43:
                decoded.append("port id=" + data[3] + ", ");
                switch (data[4]) {
                    case 0x01:
                        decoded.append("mode info, caps:(");
                        boolean isOutput = (data[5] & 0x01) != 0;
                        boolean isInput = (data[5] & 0x02) != 0;
                        boolean isLogicalCombinable = (data[5] & 0x04) != 0;
                        boolean isLogicalSynchronizable = (data[5] & 0x08) != 0;
                        if (isOutput)
                            decoded.append("output,");
                        if (isInput)
                            decoded.append("input,");
                        if (isLogicalCombinable)
                            decoded.append("logical combinable,");
                        if (isLogicalSynchronizable)
                            decoded.append("logical synchronizable,");
                        decoded.append("), number_of_modes=" + data[6] + ", ");
                        decoded.append("available_input_modes=" + data[7] + "," + data[8] + ", ");
                        decoded.append("available_output_modes=" + data[9] + "," + data[10] + ", ");

                        break;
                    case 0x02:
                        decoded.append("mode combination info, ");
                        break;
                }
                break;
            case 0x44:
                int pos3 = 3;
                decoded.append("port id=" + data[pos3++] + ", ");
                decoded.append("mode=" + data[pos3++] + ", ");
                byte modeInformationType = data[pos3++];
                decoded.append("mode_information_type=" + modeInformationTypeToString(modeInformationType) + ", ");
                switch (modeInformationType) {
                    case 0x0:
                        decoded.append("name=" + str(data, pos3));
                        break;
                    case 0x1:
                        decoded.append("RAW range, min=" + floatFromBytes(data, pos3) + ", max=" + floatFromBytes(data, pos3 + 4));
                        break;
                    case 0x2:
                        decoded.append("percent range, min=" + floatFromBytes(data, pos3) + ", max=" + floatFromBytes(data, pos3 + 4));
                        break;
                    case 0x3:
                        decoded.append("SI range, min=" + floatFromBytes(data, pos3) + ", max=" + floatFromBytes(data, pos3 + 4));
                        break;
                    case 0x4:
                        decoded.append("symbol, text=" + str(data, pos3));
                        break;
                    case 0x5:
                        decoded.append("mapping, input=" + decodeMapping(data[pos3]) + ", output=" + decodeMapping(data[pos3+1]));
                        break;
                    case 0x7:
                        decoded.append("motor bias, value=" + data[pos3]);
                        break;
                    case 0x8:
                        decoded.append("capability bits");
                        break;
                    case (byte)0x80:
                        decoded.append("value format, no_datasets=" + data[pos3] + ", type=" + data[pos3+1] + ", total_figures=" + data[pos3+2] + ", decimals=" + data[pos3+3]);
                        break;
                }


                break;
            case 0x45:
                int pos2 = 3;
                // TODO: repeat
                decoded.append("port id=" + data[pos2] + ", ");
                // TODO (this only becomes understandable, when PortModeInformationRequests are implemented

                break;

        }

        return new UnhandledResponse(decoded.toString());
    }

    private static Set<MappingOptions> decodeMapping(byte datum) {
        Set<MappingOptions> res = new HashSet<>();
        for (MappingOptions value : MappingOptions.values()) {
            if ((datum & (1 << value.getBit())) != 0)
                res.add(value);
        }
        return res;
    }

    private static String str(byte[] data, int offset) {
        byte length = data[0];

        int strlen = 0;
        while (strlen < length - offset && data[offset + strlen] != 0)
            strlen++;
        return new String(data, offset, strlen, StandardCharsets.US_ASCII);
    }

    private static String floatFromBytes(byte[] buffer, int n) {
        return "" + Float.intBitsToFloat( buffer[n] ^ buffer[n+1]<<8 ^ buffer[n+2]<<16 ^ buffer[n+3]<<24 );
    }

    private static String modeInformationTypeToString(byte value) {
        for (ModeInformationType mit : ModeInformationType.values())
            if (mit.getValue() == value)
                return mit.name();
        return "unknown (" + value + ")";
    }

    private static String versionToString(int version) {
        // 00100000.00000000.00000000.00010000
        // 0MMMmmmm.BBBBBBBB.bbbbbbbb.bbbbbbbb
        byte v[] = ByteBuffer.allocate(4).putInt(version).array();
        int major = v[0] >> 4 & 0xf;
        int minor = v[0] & 0xf;
        int bugFix = v[1];
        int r = v[2] & 0xFF;
        r = (r << 8) | (v[3] & 0xFF);
        return major + "." + minor + "." + bugFix + "." + r;
    }

    private static String propertyToString(byte property) {
        switch (property) {
            case 0x01: return "Advertising Name";
            case 0x02: return "Button";
            case 0x03: return "FW Version";
            case 0x04: return "HW Version";
            case 0x05: return "RSSI";
            case 0x06: return "Battery Voltage";
            case 0x07: return "Battery Type";
            case 0x08: return "Manufacturer Name";
            case 0x09: return "Radio Firmware Version";
            case 0x0A: return "LEGO Wireless Protocol Version";
            case 0x0B: return "System Type ID";
            case 0x0C: return "H/W Network ID";
            case 0x0D: return "Primary MAC Address";
            case 0x0E: return "Secondary MAC Address";
            case 0x0F: return "Hardware Network Family";
            default: return "unknown property (" + property + ")";
        }
    }

    private static int fromByteArray(byte[] bytes, int offset) {
        return bytes[offset] << 24 | (bytes[offset + 1] & 0xFF) << 16 | (bytes[offset + 2] & 0xFF) << 8 | (bytes[offset + 3] & 0xFF);
    }

    private static String IOTypeIdToString(int r) {
        switch (r) {
            case 0x0001:
                return "Motor";
            case 0x0002:
                return "System Train Motor";
            case 0x0005:
                return "Button";
            case 0x0008:
                return "LED Light";
            case 0x0014:
                return "Voltage";
            case 0x0015:
                return "Current";
            case 0x0016:
                return "Piezo Tone (Sound)";
            case 0x0017:
                return "RGB Light";
            case 0x0022:
                return "External Tilt Sensor";
            case 0x0023:
                return "Motion Sensor";
            case 0x0025:
                return "Vision Sensor";
            case 0x0026:
                return "External Motor with Tacho";
            case 0x0027:
                return "Internal Motor with Tacho";
            case 0x0028:
                return "Internal Tilt";
            default:
                return "unknown io type id (" + r + ")";
        }

    }

    private static String portIdToString(byte portId) {
        if (portId >= 0 && portId <= 49)
            return portId + " (hub connector port)";
        if (portId >= 50 && portId <= 100)
            return portId + " (internal)";
        return portId + " (reserved)";

    }

    private static String errorCodeToString(byte errorCode) {
        switch (errorCode) {
            case 0x01: return "ACK";
            case 0x02: return "MACK";
            case 0x03: return "Buffer Overflow";
            case 0x04: return "Timeout";
            case 0x05: return "Command NOT recognized";
            case 0x06: return "Invalid use (e.g. parameter error(s))";
            case 0x07: return "Overcurrent";
            case 0x08: return "Internal ERROR";
            default: return "unknown errorCode";
        }
    }

    private static String messageTypeToString(byte messageType) {
        StringBuffer decoded = new StringBuffer();
        switch (messageType) {
            case 0x01:
                decoded.append("hub property, ");
                break;
            case 0x02:
                decoded.append("hub action, ");
                break;
            case 0x03:
                decoded.append("hub alert, ");
                break;
            case 0x04:
                decoded.append("hub attached i/o, ");
                break;
            case 0x05:
                decoded.append("generic error messages, ");
                break;
            case 0x08:
                decoded.append("H/W NetWork Commands, ");
                break;
            case 0x10:
                decoded.append("F/W Update - Go Into Boot Mode, ");
                break;
            case 0x11:
                decoded.append("F/W Update Lock memory, ");
                break;
            case 0x12:
                decoded.append("F/W Update Lock Status Request, ");
                break;
            case 0x13:
                decoded.append("F/W Lock Status, ");
                break;
            case 0x21:
                decoded.append("Port Information Request, ");
                break;
            case 0x22:
                decoded.append("Port Mode Information Request, ");
                break;
            case 0x41:
                decoded.append("Port Input Format Setup (Single), ");
                break;
            case 0x42:
                decoded.append("Port Input Format Setup (CombinedMode), ");
                break;
            case 0x43:
                decoded.append("Port Information, ");
                break;
            case 0x44:
                decoded.append("Port Mode Information, ");
                break;
            case 0x45:
                decoded.append("Port Value (Single), ");
                break;
            case 0x46:
                decoded.append("Port Value (CombinedMode), ");
                break;
            case 0x47:
                decoded.append("Port Input Format (Single), ");
                break;
            case 0x48:
                decoded.append("Port Input Format (CombinedMode), ");
                break;
            case 0x61:
                decoded.append("Virtual Port Setup, ");
                break;
            case (byte)0x81:
                decoded.append("Port Output Command, ");
                break;
            case (byte)0x82:
                decoded.append("Port Output Command Feedback, ");
                break;
            default:
                decoded.append("unknown message type, ");
                break;
        }
        return decoded.toString();
    }

}
