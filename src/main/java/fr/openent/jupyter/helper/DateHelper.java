package fr.openent.jupyter.helper;

import fr.openent.jupyter.models.Directory;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelper {
    private static final Logger log = LoggerFactory.getLogger(Directory.class);
    private static final SimpleDateFormat workspaceFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm.ss.SSS");
    private static final SimpleDateFormat repriseFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat defaultFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String tryFormat (String dateToFormat) {
        // Delete microseconds if exists
        if (dateToFormat.length() - dateToFormat.lastIndexOf('.') > 4) {
            dateToFormat = dateToFormat.substring(0, dateToFormat.lastIndexOf('.') + 4);
        }

        try {
            return workspaceFormatter.parse(dateToFormat).toInstant().toString();
        } catch (ParseException e1) {
            try {
                return repriseFormatter.parse(dateToFormat).toInstant().toString();
            } catch (ParseException e2) {
                try {
                    return defaultFormatter.parse(dateToFormat).toInstant().toString();
                } catch (ParseException e3) {
                    log.error("[Jupyter@DateHelper] " + e1.getMessage());
                    log.error("[Jupyter@DateHelper] " + e2.getMessage());
                    log.error("[Jupyter@DateHelper] " + e3.getMessage());
                    log.error("[Jupyter@DateHelper] Failed to format date '" + dateToFormat + "'");
                    return new Date().toString();
                }
            }
        }
    }
}
