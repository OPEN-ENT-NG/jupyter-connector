package fr.openent.jupyter;

import fr.openent.jupyter.controllers.CheckpointController;
import fr.openent.jupyter.controllers.DirectoryController;
import fr.openent.jupyter.controllers.FileController;
import fr.openent.jupyter.controllers.ManagerController;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

public class Jupyter extends BaseServer {

	public static final String WORKSPACE_BUS_ADDRESS = "org.entcore.workspace";
	public static final String WORKSPACE_SRC_DIRECTORY_NAME = "Base";

	public static String EXTENSION_NOTEBOOK;
	public static JsonArray EXTENSIONS_TEXT;

	public static final String ACCESS_RIGHT = "jupyter.access";

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
    final Promise<Void> promise = Promise.promise();
    super.start(promise);
    promise.future()
      .compose(e -> this.init())
      .onComplete(startPromise);
  }
  public Future<Void> init() {
		EXTENSION_NOTEBOOK = config.getJsonObject("file-extensions").getString("notebook");
		EXTENSIONS_TEXT = config.getJsonObject("file-extensions").getJsonArray("text");

		final EventBus eb = getEventBus(vertx);
    return StorageFactory.build(vertx, config)
      .compose(storageFactory -> {
        final Storage storage = storageFactory.getStorage();
        addController(new CheckpointController(eb, storage));
        addController(new DirectoryController(eb, storage));
        addController(new FileController(eb, storage));
        addController(new ManagerController(eb, storage));
        return Future.succeededFuture();
      });
	}

}
