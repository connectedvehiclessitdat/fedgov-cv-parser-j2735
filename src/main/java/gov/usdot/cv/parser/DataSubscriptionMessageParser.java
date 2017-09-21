package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import static gov.usdot.asn1.j2735.J2735Util.toFormattedDateString;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionCancel;
import gov.usdot.asn1.generated.j2735.semi.DataSubscriptionRequest;
import gov.usdot.asn1.generated.j2735.semi.GeoRegion;
import gov.usdot.asn1.generated.j2735.semi.IsdType;
import gov.usdot.asn1.generated.j2735.semi.VsmType;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.oss.asn1.AbstractData;

public class DataSubscriptionMessageParser extends AbstractCVMessageParser {

	private static final Logger logger = Logger.getLogger(DataSubscriptionMessageParser.class);

	public static final String SUBSCRIBER_ID 	= "subscriberId";
	public static final String DIALOG_ID 		= "dialogId";
	public static final String SEQUENCE_ID 		= "sequenceId";
	public static final String REQUEST_ID 		= "requestId";
	public static final String GROUP_ID 		= "groupId";
	public static final String CERTIFICATE		= "certificate";
	public static final String DEST_HOST 		= "destHost";
	public static final String DEST_PORT 		= "destPort";
	public static final String FROM_FORWARDER 	= "fromForwarder";
	public static final String END_TIME 		= "endTime";
	public static final String TYPE 			= "type";
	public static final String TYPE_VALUE 		= "typeValue";
	public static final String NW_LAT 			= "nwLat";
	public static final String NW_LON 			= "nwLon";
	public static final String SE_LAT 			= "seLat";
	public static final String SE_LON 			= "seLon";

	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle) throws ParsePipelineException {

		if (! (message instanceof DataSubscriptionRequest) && ! (message instanceof DataSubscriptionCancel)) {
			String errorMsg = String.format(
				"Parser %s does not support messages of type %s",
				this.getClass().getName(), 
				message.getClass().getName());
			logger.error(errorMsg);
			throw new ParsePipelineException(errorMsg);
		}
		
		Map<String, String> result = null;
		if (message instanceof DataSubscriptionRequest) {
			logger.debug("Parsing subscription add request: " + message.toString());
			result = parseRequest(message);
		} else if (message instanceof DataSubscriptionCancel) {
			logger.debug("Parsing subscription cancel request: " + message.toString());
			result = parseCancel(message);
		}
		
		result.put(DEST_HOST, bundle.getDestHost());
		result.put(DEST_PORT, Integer.toString(bundle.getDestPort()));
		result.put(FROM_FORWARDER, Boolean.toString(bundle.fromForwarder()));
		result.put(CERTIFICATE, Base64.encodeBase64String(bundle.getCertificate()));
		
		return result;
	}
	
	private Map<String, String> parseRequest(AbstractData message) throws ParsePipelineException {
		HashMap<String, String> map = new HashMap<String, String>();
		DataSubscriptionRequest msg = (DataSubscriptionRequest) message;

		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, Integer.toString(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		map.put(END_TIME, toFormattedDateString(msg.getEndTime(), DATE_PATTERN));
		
		String type = null;
		int val = -1;
		if (msg.getType().hasVsmType()) {
			VsmType vsmType = (VsmType) msg.getType().getChosenValue();
			type = vsmType.getClass().getSimpleName();
			val = Byte.valueOf(vsmType.byteArrayValue()[0]).intValue();
		} else {
			IsdType isdType = (IsdType) msg.getType().getChosenValue();
			type = isdType.getClass().getSimpleName();
			val = Byte.valueOf(isdType.byteArrayValue()[0]).intValue();
		}
		map.put(TYPE, type);
		map.put(TYPE_VALUE, String.valueOf(val));
		
		GeoRegion region = msg.getServiceRegion();
		if (region != null) {
			if (region.getNwCorner() != null) {
				map.put(NW_LAT, String.valueOf(convertGeoCoordinateToDouble(region.getNwCorner().getLat().intValue())));
				map.put(NW_LON, String.valueOf(convertGeoCoordinateToDouble(region.getNwCorner().get_long().intValue())));
			}
			if (region.getSeCorner() != null) {
				map.put(SE_LAT, String.valueOf(convertGeoCoordinateToDouble(region.getSeCorner().getLat().intValue())));
				map.put(SE_LON, String.valueOf(convertGeoCoordinateToDouble(region.getSeCorner().get_long().intValue())));
			}
		}

		return map;
	}
	
	private Map<String, String> parseCancel(AbstractData message) throws ParsePipelineException {
		HashMap<String, String> map = new HashMap<String, String>();
		DataSubscriptionCancel msg = (DataSubscriptionCancel) message;

		map.put(SUBSCRIBER_ID, String.valueOf(ByteBuffer.wrap(msg.getSubID().byteArrayValue()).getInt()));
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, String.valueOf(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		
		return map;
	}

}
