package gov.usdot.cv.parser;

import gov.usdot.asn1.generated.j2735.dsrc.DDateTime;
import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.j2735.J2735Util;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.oss.asn1.AbstractData;
import com.deleidos.rtws.core.framework.parser.ParsePipelineException;

public abstract class AbstractCVMessageParser implements CVMessageParser {

	private static final Logger logger = Logger.getLogger(AbstractCVMessageParser.class);

	protected static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
	
	protected void validateMessageType(AbstractData message, Class<?> clazz) throws ParsePipelineException {
		if (!clazz.isInstance(message)) {
			String errorMsg = String.format("Parser %s does not support messages of type %s",
					this.getClass().getName(), message.getClass().getName());
			logger.error(errorMsg);
			throw new ParsePipelineException(errorMsg);
		}
	}

	protected String buildPathHistoryJSON(int index, short latOffset, short longOffset, short elevationOffset,
			short timeOffset) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.element("latOffset", latOffset);
		jsonObj.element("longOffset", longOffset);
		jsonObj.element("elevationOffset", elevationOffset);
		jsonObj.element("timeOffset", timeOffset);
		return jsonObj.toString();
	}

	protected JSONObject buildDateTimeJSON(DDateTime dateTime) {
		JSONObject jsonObj = new JSONObject();
		if (dateTime.getYear() != null)
			jsonObj.element("year", dateTime.getYear().intValue());
		if (dateTime.getMonth() != null)
			jsonObj.element("month", dateTime.getMonth().intValue());
		if (dateTime.getDay() != null)
			jsonObj.element("day", dateTime.getDay().intValue());
		if (dateTime.getHour() != null)
			jsonObj.element("hour", dateTime.getHour().intValue());
		if (dateTime.getMinute() != null)
			jsonObj.element("minute", dateTime.getMinute().intValue());
		if (dateTime.getSecond() != null)
			jsonObj.element("second", dateTime.getSecond().intValue());
		if ( dateTime.hasOffset() )
			jsonObj.element("offset", dateTime.getOffset().intValue());
		return jsonObj;
	}

	protected JSONObject buildPositionJSON(Position3D pos) {
		JSONObject jsonObj = new JSONObject();
		jsonObj.element("lat", J2735Util.convertGeoCoordinateToDouble(pos.getLat().intValue()));
		jsonObj.element("lon", J2735Util.convertGeoCoordinateToDouble(pos.get_long().intValue()));
		if (pos.getElevation() != null) {
			jsonObj.element("elevation", String.valueOf(pos.getElevation().longValue()));
		}
		return jsonObj;
	}
	
}
