package gov.usdot.cv.parser;

import gov.usdot.asn1.generated.j2735.dsrc.DDateTime;
import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage.Bundle;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.dialog.DataBundle;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.oss.asn1.AbstractData;
import com.deleidos.rtws.core.framework.parser.ParsePipelineException;

public class VehSitDataMessageParser extends AbstractCVMessageParser {
	
	private static final String DIALOG_ID 		= "dialogId";
	private static final String SEQUENCE_ID 	= "sequenceId";
	private static final String GROUP_ID		= "groupId";	
	private static final String REQUEST_ID		= "requestId";
	private static final String VSM_TYPE 		= "vsmType";
	private static final String D_YEAR 			= "DYear";
	private static final String D_MONTH 		= "DMonth";
	private static final String D_DAY 			= "DDay";
	private static final String D_HOUR 			= "DHour";
	private static final String D_MINUTE 		= "DMinute";
	private static final String D_SECOND 		= "DSecond";
	private static final String D_OFFSET 		= "DOffset";	
	private static final String LONG 			= "Long";
	private static final String LAT 			= "Lat";
	private static final String ELEVATION 		= "Elevation";
	private static final String COUNT 			= "Count";
	private static final String ENCODED_MSG 	= "encodedMsg";
	
	public Map<String, String> parse(AbstractData message, DataBundle dataBundle) throws ParsePipelineException {
		String encodedMsg = dataBundle.encodePayload();
		
		validateMessageType(message, VehSitDataMessage.class);
		VehSitDataMessage msg = (VehSitDataMessage)message;
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(ENCODED_MSG, encodedMsg);
		map.put(DIALOG_ID, String.valueOf(msg.getDialogID().longValue()));
		map.put(SEQUENCE_ID, String.valueOf(msg.getSeqID().longValue()));
		map.put(GROUP_ID, String.valueOf(GroupIDHelper.fromGroupID(msg.getGroupID())));
		map.put(REQUEST_ID, String.valueOf(ByteBuffer.wrap(msg.getRequestID().byteArrayValue()).getInt()));
		Byte type_b = ByteBuffer.wrap(msg.getType().byteArrayValue()).get();
		map.put(VSM_TYPE, String.valueOf(type_b.intValue()));
		
		Bundle bundle = msg.getBundle();
		assert(bundle != null);
		int count = bundle.getSize();
		assert(count >  0);
		VehSitRecord vsr = bundle.get(0);
		parseVehSitRecord(map, vsr);

		map.put(COUNT, String.valueOf(count));
		return map;
	}
	
	private static void parseVehSitRecord(HashMap<String, String> map, VehSitRecord vsr){
		DDateTime dateTime = vsr.getTime();
		map.put(D_YEAR, String.valueOf(dateTime.getYear().longValue()));
		map.put(D_MONTH, String.valueOf(dateTime.getMonth().longValue()));
		map.put(D_DAY, String.valueOf(dateTime.getDay().longValue()));
		map.put(D_HOUR, String.valueOf(dateTime.getHour().longValue()));
		map.put(D_MINUTE, String.valueOf(dateTime.getMinute().longValue()));
		map.put(D_SECOND, String.valueOf(dateTime.getSecond().longValue()));
		if ( dateTime.hasOffset() )
			map.put(D_OFFSET,  String.valueOf(dateTime.getOffset().longValue()));
		
		Position3D pos = vsr.getPos();
		Double lat = J2735Util.convertGeoCoordinateToDouble(pos.getLat().intValue());
		map.put(LAT, String.valueOf(lat));
		Double lon = J2735Util.convertGeoCoordinateToDouble(pos.get_long().intValue());
		map.put(LONG, String.valueOf(lon));
		if ( pos.hasElevation() ) {
			map.put(ELEVATION, String.valueOf(pos.getElevation().longValue()));
		}
	}
}
