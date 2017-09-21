package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import static gov.usdot.asn1.j2735.J2735Util.toFormattedDateString;
import gov.usdot.asn1.generated.j2735.semi.GeoRegion;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.oss.asn1.AbstractData;

public class IntersectionSitDataParser extends AbstractCVMessageParser {
	
	private static final String DIALOG_ID 		= "dialogId";
	private static final String SEQUENCE_ID 	= "sequenceId";
	private static final String GROUP_ID		= "groupId";
	private static final String REQUEST_ID		= "requestId";
	private static final String BUNDLE_NUMBER 	= "bundleNumber";
	private static final String TIME_TO_LIVE	= "timeToLive";
	private static final String TIMESTAMP		= "timestamp";
	private static final String NW_LAT 			= "nwLat";
	private static final String NW_LON 			= "nwLon";
	private static final String SE_LAT 			= "seLat";
	private static final String SE_LON 			= "seLon";
	private static final String ENCODED_MSG 	= "encodedMsg";
	
	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle)
			throws ParsePipelineException {
		HashMap<String, String> map = new HashMap<String, String>();
		
		validateMessageType(message, IntersectionSituationData.class);
		
		IntersectionSituationData msg = (IntersectionSituationData) message;
		
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, Integer.toString(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		map.put(BUNDLE_NUMBER, String.valueOf(msg.getBundleNumber()));
		if (msg.hasTimeToLive()) map.put(TIME_TO_LIVE, String.valueOf(msg.getTimeToLive().longValue()));
		map.put(TIMESTAMP, toFormattedDateString(msg.getIntersectionRecord().getSpatData().getTimestamp(), DATE_PATTERN));
		
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
		
		map.put(ENCODED_MSG, bundle.encodePayload());
		
		return map;
	}
	
	
	
}