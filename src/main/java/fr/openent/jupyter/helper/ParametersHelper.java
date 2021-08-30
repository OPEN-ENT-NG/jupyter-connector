package fr.openent.jupyter.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;

public class ParametersHelper {
    public static void hasMissingParameters(String[] params, Handler<Either<String, Boolean>> handler) {
        for (String param : params) {
            if (param == null) {
                handler.handle(new Either.Left<>("Missing parameter"));
            }
        }
        handler.handle(new Either.Right<>(true));

    }

    public static void hasEmptyParameters (String[] params, Handler<Either<String, Boolean>> handler) {
        for (String param : params) {
            if (param.isEmpty()) {
                handler.handle(new Either.Left<>("Empty parameter"));
            }
        }
        handler.handle(new Either.Right<>(true));
    }

    public static void hasMissingOrEmptyParameters (String[] params, Handler<Either<String, Boolean>> handler) {
        hasMissingParameters(params, missingHandler -> {
            if (missingHandler.isRight()) {
                hasEmptyParameters(params, handler);
            }
            else {
                handler.handle(new Either.Left<>(missingHandler.left().getValue()));
            }
        });
    }

//    Potentially to improve to explain :
//    - which parameter is problematic ?
//    - is it non-existent or is it empty ?
//    - is this parameter expected in url or header ?
}
