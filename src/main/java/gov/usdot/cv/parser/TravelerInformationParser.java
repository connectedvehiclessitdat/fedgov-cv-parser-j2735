package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import static gov.usdot.asn1.j2735.J2735Util.toFormattedDateString;
import gov.usdot.asn1.generated.j2735.semi.AdvisorySituationData;
import gov.usdot.asn1.generated.j2735.semi.DistributionType;
import gov.usdot.asn1.generated.j2735.semi.GeoRegion;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.oss.asn1.AbstractData;

public class TravelerInformationParser extends AbstractCVMessageParser {

	public static final String RECEIPT_ID 			= "receiptId";
	public static final String DIALOG_ID 			= "dialogId";
	public static final String SEQUENCE_ID 			= "sequenceId";
	public static final String GROUP_ID 			= "groupId";
	public static final String REQUEST_ID			= "requestId";
	public static final String RECORD_ID			= "recordId";
	public static final String TIME_TO_LIVE			= "timeToLive";
	public static final String NW_LAT 				= "nwLat";
	public static final String NW_LON 				= "nwLon";
	public static final String SE_LAT 				= "seLat";
	public static final String SE_LON 				= "seLon";
	public static final String ASDM_ID				= "asdmId";
	public static final String ASDM_TYPE			= "asdmType";
	public static final String DIST_TYPE			= "distType";
	public static final String START_TIME			= "startTime";
	public static final String STOP_TIME			= "stopTime";
	public static final String ADVISORY_MESSAGE 	= "advisoryMessage";
	public static final String ENCODED_MSG 			= "encodedMsg";

	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle) throws ParsePipelineException {
		HashMap<String, String> map = new HashMap<String, String>();
		
		validateMessageType(message, AdvisorySituationData.class);
		
		AdvisorySituationData msg = (AdvisorySituationData) message;

		map.put(RECEIPT_ID, bundle.getReceiptId());
		map.put(ENCODED_MSG, bundle.encodePayload());
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, Integer.toString(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		
		if (msg.hasRecordID()) map.put(RECORD_ID, Integer.toString(ByteBuffer.wrap(msg.getRecordID().byteArrayValue()).getInt()));
		if (msg.hasTimeToLive()) map.put(TIME_TO_LIVE, String.valueOf(msg.getTimeToLive().longValue()));

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
		map.put(ASDM_ID, Integer.toString(ByteBuffer.wrap(msg.getAsdmDetails().getAsdmID().byteArrayValue()).getInt()));
		map.put(ASDM_TYPE, String.valueOf(msg.getAsdmDetails().getAsdmType().longValue()));
		
		DistributionType dt = msg.getAsdmDetails().getDistType();
		map.put(DIST_TYPE, String.valueOf(Byte.valueOf(dt.byteArrayValue()[0]).intValue()));
		
		if (msg.getAsdmDetails().getStartTime() != null) {
			map.put(START_TIME, toFormattedDateString(msg.getAsdmDetails().getStartTime(), DATE_PATTERN));
		}
		if (msg.getAsdmDetails().getStopTime() != null) {
			map.put(STOP_TIME, toFormattedDateString(msg.getAsdmDetails().getStopTime(), DATE_PATTERN));
		}
		map.put(ADVISORY_MESSAGE, Hex.encodeHexString(msg.getAsdmDetails().getAdvisoryMessage().byteArrayValue()));
		
		return map;
	}

}
