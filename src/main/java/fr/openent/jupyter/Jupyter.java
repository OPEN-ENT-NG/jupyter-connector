package fr.openent.jupyter;

import fr.openent.jupyter.controllers.CheckpointController;
import fr.openent.jupyter.controllers.DirectoryController;
import fr.openent.jupyter.controllers.FileController;
import fr.openent.jupyter.controllers.ManagerController;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

public class Jupyter extends BaseServer {
	private static final Logger log = LoggerFactory.getLogger(Jupyter.class);

	public static final String WORKSPACE_BUS_ADDRESS = "org.entcore.workspace";
	public static final String WORKSPACE_SRC_DIRECTORY_NAME = "Base";

	public static String EXTENSION_NOTEBOOK;
	public static JsonArray EXTENSIONS_TEXT;

	public static final String ACCESS_RIGHT = "jupyter.access";

	@Override
	public void start() throws Exception {
		super.start();

		EXTENSION_NOTEBOOK = config.getJsonObject("file-extensions").getString("notebook");
		EXTENSIONS_TEXT = config.getJsonObject("file-extensions").getJsonArray("text");

		final EventBus eb = getEventBus(vertx);
		final Storage storage = new StorageFactory(vertx, config).getStorage();

		addController(new CheckpointController(eb, storage));
		addController(new DirectoryController(eb, storage));
		addController(new FileController(eb, storage));
		addController(new ManagerController(eb, storage));
	}

}
