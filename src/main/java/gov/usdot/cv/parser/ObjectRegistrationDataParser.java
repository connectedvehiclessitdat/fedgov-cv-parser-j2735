package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import gov.usdot.asn1.generated.j2735.semi.GeoRegion;
import gov.usdot.asn1.generated.j2735.semi.ObjectRegistrationData;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.oss.asn1.AbstractData;

public class ObjectRegistrationDataParser extends AbstractCVMessageParser {
	
	private static final String RECEIPT_ID 			= "receiptId";
	private static final String DIALOG_ID 			= "dialogId";
	private static final String SEQUENCE_ID 		= "sequenceId";
	private static final String GROUP_ID			= "groupId";
	private static final String REQUEST_ID			= "requestId";
	private static final String SERVICE_ID			= "serviceId";
	private static final String SERVICE_PROVIDER_ID	= "serviceProviderId";
	
	private static final String DEST_HOST			= "destHost";
	private static final String DEST_PORT			= "destPort";
	private static final String FROM_FORWARDER		= "fromForwarder";
	private static final String CERTIFICATE			= "certificate";
	
	private static final String NW_LAT 				= "nwLat";
	private static final String NW_LON 				= "nwLon";
	private static final String SE_LAT 				= "seLat";
	private static final String SE_LON 				= "seLon";
	private static final String ENCODED_MSG 		= "encodedMsg";
	
	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle) throws ParsePipelineException {
		
		validateMessageType(message, ObjectRegistrationData.class);
		
		Map<String, String> result = parseDataRequest((ObjectRegistrationData)message);
		result.put(RECEIPT_ID, bundle.getReceiptId());
		result.put(DEST_HOST, bundle.getDestHost());
		result.put(DEST_PORT, Integer.toString(bundle.getDestPort()));
		result.put(FROM_FORWARDER, Boolean.toString(bundle.fromForwarder()));
		result.put(CERTIFICATE, Base64.encodeBase64String(bundle.getCertificate()));
		
		result.put(ENCODED_MSG, bundle.encodePayload());

		return result;
	}
	
	private Map<String, String> parseDataRequest(ObjectRegistrationData msg) {
		HashMap<String, String> map = new HashMap<String, String>();
		
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, Integer.toString(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		map.put(SERVICE_ID, String.valueOf(msg.getServiceID().longValue()));
		map.put(SERVICE_PROVIDER_ID, String.valueOf(ByteBuffer.wrap(
				msg.getServiceRecord().getSvcProvider().byteArrayValue()).getInt()));
		
		GeoRegion region = msg.getServiceRecord().getServiceRegion();
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
}