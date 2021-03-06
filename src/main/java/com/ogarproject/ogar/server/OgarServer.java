/**
 * This file is part of Ogar.
 *
 * Ogar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ogar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ogar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ogarproject.ogar.server;

import com.ogarproject.ogar.server.world.WorldImpl;
import com.skorrloregaming.main.Commands;
import com.ogarproject.ogar.api.Ogar;
import com.ogarproject.ogar.api.Server;
import com.ogarproject.ogar.api.plugin.Messenger;
import com.ogarproject.ogar.api.plugin.PluginManager;
import com.ogarproject.ogar.api.plugin.Scheduler;
import com.ogarproject.ogar.server.config.Configuration;
import com.ogarproject.ogar.server.config.OgarConfig;
import com.ogarproject.ogar.server.gui.ServerCLI;
import com.ogarproject.ogar.server.gui.ServerGUI;
import com.ogarproject.ogar.server.net.NetworkManager;
import com.ogarproject.ogar.server.plugin.SimpleScheduler;
import com.ogarproject.ogar.server.net.packet.outbound.PacketOutUpdateLeaderboardFFA;
import com.ogarproject.ogar.server.tick.TickWorker;
import com.ogarproject.ogar.server.tick.Tickable;
import com.ogarproject.ogar.server.tick.TickableSupplier;
import com.ogarproject.ogar.server.util.Versioning;
import com.ogarproject.ogar.server.world.PlayerImpl;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class OgarServer implements Server {

    private static OgarServer instance;
    public static final Logger log = Logger.getGlobal();
    private final PlayerList playerList = new PlayerList(this);
    private final VirusList virusList = new VirusList();
    private final MassList massList = new MassList();
    private final FoodList foodList = new FoodList();
    private final String configurationFile = "server.properties";
    private final boolean debugMode = Boolean.getBoolean("debug");
    private final Set<TickWorker> tickWorkers = new HashSet<TickWorker>();
    private final Messenger messenger = new Messenger();
    private final SimpleScheduler scheduler = new SimpleScheduler(this);
    private int tickThreads = Integer.getInteger("tickThreads", 1);
    private NetworkManager networkManager;
    private PluginManager pluginManager;
    private WorldImpl world;
    private OgarConfig configuration;
    private long tick = 0L;
    private boolean running;
    private final String ogarVersion = Versioning.getOgarVersion();

    public static void main(String[] args) throws Throwable {
        OgarServer.instance = new OgarServer();
        OgarServer.instance.run();
    }

    public static OgarServer getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return log;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public PlayerList getPlayerList() {
        return playerList;
    }
    
    public MassList getMassList() {
    	return massList;
    }

    public FoodList getFoodList() {
    	return foodList;
    }

    public VirusList getVirusList() {
    	return virusList;
    }

    public WorldImpl getWorld() {
        return world;
    }

    public Messenger getMessenger() {
        return messenger;
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }

    public boolean isDebugging() {
        return debugMode;
    }

    private void setupLogging() {
        log.setUseParentHandlers(false);

        LogFormatter formatter = new LogFormatter();

        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(formatter);
        if (isDebugging()) {
            log.setLevel(Level.FINEST);
            ch.setLevel(Level.FINEST);
        } else {
            log.setLevel(Level.INFO);
            ch.setLevel(Level.INFO);
        }
        log.addHandler(ch);

        try {
            FileHandler fh = new FileHandler("server.log");
            fh.setFormatter(formatter);
            if (isDebugging()) {
                fh.setLevel(Level.FINEST);
            } else {
                ch.setLevel(Level.INFO);
            }
            log.addHandler(fh);
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Error while adding FileHandler to logger. Logs will not be output to a file.", ex);
        }

    }

    public void loadConfig() {
        this.configuration = Configuration.load(configurationFile);
    }

    public void saveConfig() {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("server.properties", "UTF-8");
        } catch (Exception ex) {
            log.severe("An internal error has occured whilist generating server.properties.");
            shutdown();
        }
        writer.println("name=Unknown Server");
        writer.println("ip=localhost");
        writer.println("port=443");
        writer.println("maxPlayers=20");
        writer.println("maxCells=20");
        writer.println("maxMass=85500");
        writer.println("minMassSplit=36");
        writer.println("minMassEject=24");
        writer.println("ejectedMassSize=16");
        writer.println("foodSize=1");
        writer.println("foodStartAmount=100");
        writer.println("virusStartAmount=10");
        writer.println("virusSize=50");
        writer.println("startMass=35");
        writer.println("recombineTime=5");
        writer.println("borderSize=6000");
        writer.close();
        loadConfig();
    }

    public OgarConfig getConfig() {
        return configuration;
    }

    public long getTick() {
        return tick;
    }

    private void run() {
        if (ServerGUI.isHeadless()) {
            Thread thread = new Thread(new ServerCLI(this), "Console Command Handler");
            thread.setDaemon(true);
            thread.start();
        } else {
            ServerGUI.spawn(this);
        }
        Ogar.setServer(this);
        pluginManager = new PluginManager(this);
        setupLogging();
        log.info("Ogar 2 by OgarioProject is now starting.");
        if (debugMode) {
            log.info("Debug mode is enabled; additional information will be logged.");
        }
        // Create the tick workers
        if (tickThreads < 1) {
            tickThreads = 1;
        }
        if (tickThreads > 1) {
            log.warning("Use of multiple tick threads is experimental and may be unstable!");
        }
        for (int i = 0; i < tickThreads; i++) {
            tickWorkers.add(new TickWorker());
        }
        if (!new File(configurationFile).isFile()) {
            saveConfig();
        }else{
            loadConfig();
        }
        world = new WorldImpl(this);
        log.info("Loading plugins.");
        try {
            File pluginDirectory = new File("plugins");
            if (!pluginDirectory.exists()) {
                pluginDirectory.mkdirs();
            }
            pluginManager.loadPlugins(pluginDirectory);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Failed to load plugins", t);
        }
        log.info("Enabling plugins.");
        pluginManager.enablePlugins();
        log.info("Start listening on port " + getConfig().server.port);
        networkManager = new NetworkManager(this);
        try {
            networkManager.start();
        } catch (IOException | InterruptedException ex) {
            log.info("Failed to start server! "+ex.getMessage());
            ex.printStackTrace();
            if (ServerGUI.isSpawned()) {
            	System.exit(1);
            } else {
                System.exit(1);
            }
        }
        tickWorkers.forEach(TickWorker::start);
        running = true;
        while (running) {
            try {
                long startTime = System.currentTimeMillis();
                tick++;
                world.tick(this::tick);
                for (PlayerImpl player : playerList.getAllPlayers()) {
                    tick(player.getTracker()::updateNodes);
                }
                for(PlayerImpl player : instance.getPlayerList().getAllPlayers())
                    player.getConnection().sendPacket(new PacketOutUpdateLeaderboardFFA(instance));
                tickWorkers.forEach(TickWorker::waitForCompletion);
                scheduler.serverTick(tick);
                long tickDuration = System.currentTimeMillis() - startTime;
                if (tickDuration < 50) {
                    log.finer("Tick took " + tickDuration + "ms, sleeping for a bit");
                    Thread.sleep(50 - tickDuration);
                } else {
                    log.finer("Tick took " + tickDuration + "ms (which is >=50ms), no time for sleep");
                }
            } catch (InterruptedException ex) {
                break;
            }
        }
        tickWorkers.forEach(TickWorker::shutdownGracefully);
        tickWorkers.forEach(TickWorker::waitForShutdown);
        networkManager.shutdown();
        log.info("Disabling plugins...");
        pluginManager.disablePlugins();
        log.info("Successfully stopped server!");
        try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.exit(-1);
    }

    public void handleCommand(String s) {
        s = s.trim();
        if (s.isEmpty()) {
            return;
        }
        Commands.onCommand(s);
    }

    public void shutdown() {
        running = false;
    }

    private void tick(Tickable... tickables) {
        for (Tickable t : tickables) {
            TickWorker bestWorker = null;
            for (TickWorker w : tickWorkers) {
                if (bestWorker == null) {
                    bestWorker = w;
                    continue;
                }
                if (w.getObjectsRemaining() < bestWorker.getObjectsRemaining()) {
                    bestWorker = w;
                }
            }
            bestWorker.tick(t);
        }
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private void tick(Supplier... suppliers) {
        for (Supplier s : suppliers) {
            tick(new TickableSupplier(s));
        }
    }

    private static class LogFormatter extends Formatter {

        private static final DateFormat df = new SimpleDateFormat("HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder(df.format(new Date(record.getMillis())));
            sb.append(" [");
            sb.append(record.getLevel());
            sb.append("] ");
            sb.append(formatMessage(record));
            sb.append('\n');
            return sb.toString();
        }
    }

    @Override
    public String getIp() {
        return configuration.server.ip;
    }
    
    public int getPort() {
    	return configuration.server.port;
    }
    
    public String getServerName() {
    	return configuration.server.name;
    }
    
    public String getOgarVersion() {
    	return ogarVersion;
    }
}
