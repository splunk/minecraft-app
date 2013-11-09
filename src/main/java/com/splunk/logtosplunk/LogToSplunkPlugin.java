/**
 * Copyright 2013 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/*
 * LogToSplunkPlugin provides additional logging functionality to the CraftBukkit
 * Minecraft server. The logs are written to the console and the Minecraft server
 * log, and to any number of Splunk TCP input ports declared in config.yml.
 *
 * The messages conform to Splunk's logging best practices:
 *
 *     http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6
 */
package com.splunk.logtosplunk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.block.Block;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.PortalCreateEvent;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;


public class LogToSplunkPlugin extends JavaPlugin implements Listener {
    // Store the last message logged so we can avoid logging
    // multiple times if an event incorrectly fires multiple times.
    private String lastLoggedMessage;

    //private final Queue<String> dataToBeSentToSplunk = new LinkedList<String>();

    //Values set in config file
    private Integer reconnectTime = 30; // interval between attempts to reconnect to splunkk
    private boolean doWoodTypes = false; // true if you want to use getID method to get specific types of wood (getID is deprecated)

    //below keeps track of socket status, address, and data to be sent.
    private String[] instances;
    private Queue<SplunkConnectionInstance> splunkInstances = new LinkedList<SplunkConnectionInstance>();

    @Override
    public void onEnable() {
        // Register this class with CraftBukkit as an event handler.
        getServer().getPluginManager().registerEvents((Listener) this, this);

        // set values specified in the config.yml file,
        //or assign default values (splunk server host:ports MUST
        // be specified in config.
        setConfigValues();

        //initial attempt to connect to splunk servers
        //creates a thread for each splunk instance
        establishConnectionToSplunk();

        // Write a configuration file if none exists.
        saveDefaultConfig();
    }

    // puts each SplunkConnectionInstance in a thread and runs them
    // reports some information about the success of connections.
    private void establishConnectionToSplunk() {
        boolean connectionOK = true;

        if (splunkInstances.size() > 0) {
            for (SplunkConnectionInstance i : splunkInstances) {
                Thread t = new Thread(i);
                t.start();

            }
        } else {
            connectionOK = false;
        }
        
        //give started threads a chance to run
        //so we can better attempt to report accurate status
        try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		
			e.printStackTrace();
		}
        for(SplunkConnectionInstance i : splunkInstances){
        	if(!i.connected) connectionOK = false;
        }

        // message only happens at start of mc server if any splunk connections
        // fail (or if there are no splunk connections)
        if (!connectionOK) {
            getLogger()
                .severe("Problems connecting to splunk,  if connections\n are not " +
                "established, some data may not be logged.\n" +
                "Perhaps the splunk host:port has not been set in config.");
        }

        //send a message to the splunk server that connection was established
        if (connectionOK) {
            writeMessage("Minecraft server started with connection to splunk.");
        }
    }
    
    /**
     * keeps track of the state of a particular splunk instance
     *
     */
    class SplunkConnectionInstance implements Runnable {
        final String host;
        final int port;
        Socket socket;
        boolean connected;
        private final Queue<String> data = new LinkedList<String>();

        SplunkConnectionInstance(String host, int port) {
            this.host = host;
            this.port = port;
            connected = false;
        }

        public void run() {
            while (true) {
                if (connect()) {
                    sendData();
                }

                try {
                    Thread.sleep(1000 * reconnectTime);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void sendData() {
            sendPackage(socket, data);
        }

		public boolean connect() {
			boolean wasConnected = (socket == null) ? false : true;

			socket = getSocket();

			connected = (socket == null) ? false : true;

			if (connected && !wasConnected) {
				getLogger().info(
						"LogToSplunk connected with Splunk instance " + host
								+ ":" + port);

			}

			return connected;
		}

        public Socket getSocket() {
            try {
                return new Socket(host, port);
            } catch (UnknownHostException e) {
                getLogger().severe(e.getMessage() + " *at " + host + ":" + port);

                return null;
            } catch (IOException e) {
                getLogger().severe(e.getMessage() + " *at " + host + ":" + port);

                return null;
            }
        }
    }
    
    //synchronized to prevent concurrent modification of data buffers
    private synchronized void sendPackage(final Socket s,
        final Queue<String> instanceBuffer) {
        while (instanceBuffer.size() > 0) {
            if (!sendMessageToSplunk(s, instanceBuffer.poll())) {
                return;
            }
        }
    }
    
    // sends a stamped message to a single splunk server. 
    // is called repeatedly by the sendPackageToSplunk method until all have
    // been sent.
    private boolean sendMessageToSplunk(Socket s, String stampedMsg) {
        try {
            s.getOutputStream().write(stampedMsg.getBytes("UTF-8"));

            return true;
        } catch (UnsupportedEncodingException e) {
            getLogger().severe(e.getMessage());
            // e.printStackTrace();
            writeMessage(stampedMsg);

            return false;
        } catch (IOException e) {
            getLogger().severe(e.getMessage());
            writeMessage(stampedMsg);

            // e.printStackTrace();
            return false;
        } catch (Exception e) {
            //we dont want to lose any data
            getLogger().severe(e.getMessage());
            writeMessage(stampedMsg);

            return false;
        }
    }
    
    
    // prepares the string that will be sends to splunk, and sends
    // it off to the data buffer of each splunk instance
    private void writeMessage(String msg) {
        String stampedMsg = Calendar.getInstance().getTime().toString() + " " +
            msg + "\r\n\r\n";

        if (stampedMsg.equals(lastLoggedMessage)) {
            return;
        } else {
            lastLoggedMessage = stampedMsg;
        }

        //each time we get a message, write it to the list to be sent
        bundleMessage(stampedMsg);
    }


    //puts the message in the data buffer for each SplunkConnectionInstance
    private void bundleMessage(String stampedMsg) {
        for (SplunkConnectionInstance s : splunkInstances) {
            Queue<String> instanceDataBuffer = s.data;
            instanceDataBuffer.offer(stampedMsg);
        }
    }

    /**
     * assigns values for some members of this class that can be set in config
     * currently: instances(splunk server host ports), doWoodTypes -to
     * have splunk specifiy specific wood types like "Birch log" rather than just "log"
     * and splunkinterval - the number of seconds between each attemp to send
     * data to the splunk server.
     */
    private void setConfigValues() {

    	prepareAttemptToReconnectTime();


        prepareWoodBoolean();


        prepareSplunkInstances();
    }
    
    
    // Extract the Splunk TCP sockets to write to from the configuration.
    // The sockets are specified as a comma separated list of host:port
    // pairs, e.g.,
    //
    //     boris.local:10000, hilda:1234
    //
    // Whitespace around the commas are ignored.
    //creates a SplunkConnectionInstances for each connection specified in config
    private void prepareSplunkInstances() {
        String splunks = getConfig().getString("splunks");

        if (splunks != null) {
            instances = splunks.split(",");

            for (int i = 0; i < instances.length; i++) {
                String[] pieces = instances[i].split(":");
                String host = pieces[0].trim();
                int port = Integer.valueOf(pieces[1].trim());

                splunkInstances.add(new SplunkConnectionInstance(host, port));


            }
        }
    }
    
    //sets the time interval between attempts to send data
    //default is 30s
    private void prepareAttemptToReconnectTime(){
        try {
            Integer seconds = new Integer(getConfig().getString("splunkinterval"));
            reconnectTime = seconds;
            getLogger()
                .info("Time between connect attemps to splunk: " +
                reconnectTime +
                "s \n Set in config under heading 'splunkinterval:'");
        } catch (NumberFormatException e) {
            getLogger()
                .severe("Connect to splunk interval not a assigned a proper integer\n in config, default value of 30s" +
                "between connect attemps assigned.");
        }
    }
    
    //sets if specific wood names are used:
    //default set to false
    private void prepareWoodBoolean(){
        Boolean b = new Boolean(getConfig().getString("usewoodtypes"));
        doWoodTypes = b;

        String yesNo = (doWoodTypes) ? "" : "not ";
        getLogger()
            .info("Specific wood types will " + yesNo +
            "be collected by splunk\n to" +
            " change: set 'usewoodtypes' in config");
    }

    @Override
    public void onDisable() {
        // Close all the sockets when this plugin is disabled.
        for (SplunkConnectionInstance s : splunkInstances) {
            try {
                s.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

  

    // The types of wood are kept in a bitmask in an integer field.
    // Extract them to something usable for logging.
    private String woodDataToType(int data) {
        int woodType = data % 4;

        if (woodType == 0) {
            return "OAK";
        } else if (woodType == 1) {
            return "SPRUCE";
        } else if (woodType == 2) {
            return "BIRCH";
        } else if (woodType == 3) {
            return "JUNGLE";
        } else {
            return "UNKNOWN";
        }
    }

    private String blockName(Material material, int data) {
        // Certain wood items need to be handled specially so we can
        // log what *type* of wood they are (jungle, spruce, etc.).
        //log!! no pun intended...
        if (!doWoodTypes) {
            return material.toString();
        } else {
            if (material.getId() == Material.LOG.getId()) {
                return woodDataToType(data) + "_LOG";
            } else if (material.getId() == Material.WOOD_STEP.getId()) {
                return woodDataToType(data) + "_SLAB";
            } else if (material.getId() == Material.WOOD_STAIRS.getId()) {
                return woodDataToType(data) + "_STAIRS";
            } else if (material.getId() == Material.WOOD_DOUBLE_STEP.getId()) {
                return woodDataToType(data) + "_SLAB";
            } else {
                // Everything else, we can just log directly.
                return material.toString();
            }
        }
    }

    private String locationToString(Location location) {
        return "world=" + location.getWorld().getName() + " " + "x=" +
        location.getX() + " " + "y=" + location.getY() + " " + "z=" +
        location.getZ() + " " + "game_time=" + location.getWorld().getTime();
    }

    /////////////////////////////////////
    // Track breaking and placing blocks
    /////////////////////////////////////
    @EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
        Block b = evt.getBlock();
        StringBuilder msg = new StringBuilder();
        msg.append("action=block_broken player=" +
            evt.getPlayer().getDisplayName() + " " +
            locationToString(b.getLocation()) + " block_type=" +
            blockName(b.getType(), b.getData()));

        for (org.bukkit.inventory.ItemStack st : evt.getBlock().getDrops()) {
            msg.append(" dropped=" + st.getAmount() + "x" +
                blockName(st.getType(), st.getData().getData()));
        }

        writeMessage(msg.toString());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent evt) {
        Block b = evt.getBlock();
  
        writeMessage("action=block_placed player=" +
            evt.getPlayer().getDisplayName() + " " +
            locationToString(b.getLocation()) + " block_type=" +
            blockName(b.getType(), b.getData()));
    }

    ////////////////////
    // Track players
    ////////////////////
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent evt) {
        double fromX = evt.getFrom().getX();
        double fromY = evt.getFrom().getY();
        double fromZ = evt.getFrom().getZ();
        double toX = evt.getTo().getX();
        double toY = evt.getTo().getY();
        double toZ = evt.getTo().getZ();

        // To avoid spamming with too many events, only log when
        // a player cross a 1m boundary (i.e., from 2.6 to 3.1, but not
        // 3.1 to 3.5).
        if ((Math.floor(fromX) != Math.floor(toX)) ||
                (Math.floor(fromY) != Math.floor(toY)) ||
                (Math.floor(fromZ) != Math.floor(toZ))) {
            writeMessage("action=player_moved player=" +
                evt.getPlayer().getDisplayName() + " world=" +
                evt.getFrom().getWorld().getName() + " from_x=" +
                evt.getFrom().getX() + " from_y=" + evt.getFrom().getY() +
                " from_z=" + evt.getFrom().getZ() + " to_x=" +
                evt.getTo().getX() + " to_y=" + evt.getTo().getY() + " to_z=" +
                evt.getTo().getZ() + " game_time=" +
                evt.getTo().getWorld().getTime());
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent evt) {
        writeMessage("action=player_changed_world player=" +
            evt.getPlayer().getDisplayName() + " from_world=" +
            evt.getFrom().getName() + " to_world=" +
            evt.getPlayer().getLocation().getWorld().getName() + " to_x=" +
            evt.getPlayer().getLocation().getX() + " to_y=" +
            evt.getPlayer().getLocation().getY() + " to_z=" +
            evt.getPlayer().getLocation().getZ() + " game_time=" +
            evt.getPlayer().getLocation().getWorld().getTime());
    }

    //////////////////////////
    // Login and logout events
    //////////////////////////
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        writeMessage("action=player_connect player=" +
            evt.getPlayer().getDisplayName() + " " +
            locationToString(evt.getPlayer().getLocation()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt) {
        writeMessage("action=player_disconnect player=" +
            evt.getPlayer().getDisplayName() + " reason=\"" +
            evt.getQuitMessage() + "\"" + " " +
            locationToString(evt.getPlayer().getLocation()));
    }

    @EventHandler
    public void onPlayerKicked(PlayerKickEvent evt) {
        writeMessage("action=player_kicked player=" +
            evt.getPlayer().getDisplayName() + " reason=\"" + evt.getReason() +
            "\"" + " " + locationToString(evt.getPlayer().getLocation()));
    }

    /////////////////
    // Slaughter
    /////////////////
    private String nameOfEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getDisplayName();
        } else {
            return entity.getType().getName().toLowerCase();
        }
    }

    // onEntityDamaged is used to log deaths in the world.
    @EventHandler
    public void onEntityDamaged(final EntityDamageEvent evt) {
        // If an entity is going to die, it won't until after onEntityDamaged has already
        // fired. So we have to schedule a task to run afterwards to determine if the entity
        // actually died or not.
        Bukkit.getScheduler().scheduleSyncDelayedTask(this,
            new Runnable() {
                public void run() {
                    // Only log deaths of living things that have actually died.
                    if (evt.getEntity() instanceof LivingEntity &&
                            evt.getEntity().isDead()) {
                        StringBuilder msg = new StringBuilder();

                        if (evt.getEntity() instanceof Player) {
                            msg.append("action=player_died");
                        } else {
                            msg.append("action=mob_died");
                        }

                        msg.append(" victim=" +
                            nameOfEntity((LivingEntity) evt.getEntity()));

                        if (evt instanceof EntityDamageByEntityEvent) {
                            Entity damager = ((EntityDamageByEntityEvent) evt).getDamager();

                            if (damager instanceof Projectile) {
                                msg.append(" killer=" +
                                    nameOfEntity(
                                        ((Projectile) damager).getShooter()));
                            } else if (damager instanceof LivingEntity) {
                                msg.append(" killer=" +
                                    nameOfEntity((LivingEntity) damager));
                            }
                        } else {
                            msg.append(" killer=" +
                                evt.getCause().toString().toLowerCase());
                        }

                        msg.append(" " +
                            locationToString(evt.getEntity().getLocation()));
                        writeMessage(msg.toString());
                    }
                }
            });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent evt) {
        StringBuilder msg = new StringBuilder();
        msg.append("action=player_respawn player=" +
            evt.getPlayer().getDisplayName());
        msg.append(" " + locationToString(evt.getPlayer().getLocation()));
        writeMessage(msg.toString());
    }

    //////////////
    // Chat log
    //////////////
    @EventHandler
    public void onChat(AsyncPlayerChatEvent evt) {
        StringBuilder msg = new StringBuilder();
        msg.append("action=chat player=" + evt.getPlayer().getDisplayName());
        msg.append(" " + locationToString(evt.getPlayer().getLocation()));
        msg.append(" message=\"" + evt.getMessage() + "\"");
        writeMessage(msg.toString());
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent evt) {
        StringBuilder msg = new StringBuilder();
        msg.append("action=portal_create reason=\"" + evt.getReason() + "\"");
        msg.append(" " +
            locationToString(evt.getBlocks().get(0).getLocation()));
        writeMessage(msg.toString());
    }

    class HostPortCombo {
        final String host;
        final int port;

        HostPortCombo(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

   
}
