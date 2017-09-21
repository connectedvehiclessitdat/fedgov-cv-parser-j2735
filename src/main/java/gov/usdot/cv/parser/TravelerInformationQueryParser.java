package gov.usdot.cv.parser;

import static gov.usdot.asn1.j2735.J2735Util.convertGeoCoordinateToDouble;
import gov.usdot.asn1.generated.j2735.semi.GeoRegion;
import gov.usdot.asn1.generated.j2735.semi.DataRequest;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.deleidos.rtws.core.framework.parser.ParsePipelineException;
import com.oss.asn1.AbstractData;

public class TravelerInformationQueryParser extends AbstractCVMessageParser {
	
	private static final Logger logger = Logger.getLogger(TravelerInformationQueryParser.class);
	
	public static final String RECEIPT_ID 			= "receiptId";
	public static final String DIALOG_ID 			= "dialogId";
	public static final String SEQUENCE_ID 			= "sequenceId";
	public static final String GROUP_ID				= "groupId";
	public static final String REQUEST_ID			= "requestId";
	public static final String DEST_HOST			= "destHost";
	public static final String DEST_PORT			= "destPort";
	public static final String FROM_FORWARDER		= "fromForwarder";
	public static final String CERTIFICATE			= "certificate";
	public static final String NW_LAT 				= "nwLat";
	public static final String NW_LON 				= "nwLon";
	public static final String SE_LAT 				= "seLat";
	public static final String SE_LON 				= "seLon";
	public static final String TIME_BOUND			= "timeBound";
	
	public Map<String, String> parse(
			AbstractData message, 
			DataBundle bundle) throws ParsePipelineException {
		
		if (! (message instanceof DataRequest) ) {
			String errorMsg = String.format(
				"Parser %s does not support messages of type %s",
				this.getClass().getName(), 
				message.getClass().getName());
			logger.error(errorMsg);
			throw new ParsePipelineException(errorMsg);
		}
		
		SemiDialogID dialogID = ((DataRequest)message).getDialogID();
		if ( dialogID == SemiDialogID.advSitDatDist ) {
			logger.debug("Parsing advisory situation data request: " + message.toString());
		} else if ( dialogID == SemiDialogID.intersectionSitDataQuery ) {
			logger.debug("Parsing intersection situation data request: " + message.toString());
		} else {
			String errorMsg = String.format(
					"Parser recieved DataRequest message with unexpected dialogID value %s (%d,0x%x)",
					dialogID.toString(), (int)dialogID.longValue(), (int)dialogID.longValue());
			logger.error(errorMsg);
			throw new ParsePipelineException(errorMsg);
		}
		
		Map<String, String> result = parseDataRequest(message);
		
		result.put(RECEIPT_ID, bundle.getReceiptId());
		result.put(DEST_HOST, bundle.getDestHost());
		result.put(DEST_PORT, Integer.toString(bundle.getDestPort()));
		result.put(FROM_FORWARDER, Boolean.toString(bundle.fromForwarder()));
		result.put(CERTIFICATE, Base64.encodeBase64String(bundle.getCertificate()));
		
		return result;
	}
	
	private Map<String, String> parseDataRequest(AbstractData message) {
		HashMap<String, String> map = new HashMap<String, String>();
		
		DataRequest msg = (DataRequest) message;
		
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, Integer.toString(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		if (msg.hasTimeBound()) 
			map.put(TIME_BOUND, String.valueOf(msg.getTimeBound()));
		
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
	
}