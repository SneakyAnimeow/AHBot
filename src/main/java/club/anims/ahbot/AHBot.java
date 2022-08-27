package club.anims.ahbot;

import com.google.gson.Gson;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.apache.ApacheHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.*;

public class AHBot {
    /**
     * The Factory of embeds.
     */
    public static class AhBotEmbedFactory {
        /**
         * Creates a new embed.
         * @param title The title of the embed.
         * @param description The description of the embed.
         * @return New embed.
         */
        public static MessageEmbed createEmbed(String title, String description) {
            return new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Color.CYAN)
                    .setThumbnail("https://hypixel.net/attachments/1621344745339-png.2560259")
                    .build();
        }

        /**
         * Creates a new embed.
         * @param title The title of the embed.
         * @param description The description of the embed.
         * @param fields The fields of the embed.
         * @return New embed.
         */
        public static MessageEmbed createEmbed(String title, String description, Map<String, String> fields) {
            var embedBuilder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Color.CYAN)
                    .setThumbnail("https://hypixel.net/attachments/1621344745339-png.2560259");
            fields.forEach((k,v) -> embedBuilder.addField(k, v, false));

            return embedBuilder.build();
        }
    }

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AHBot.class);

    /**
     * The AHBot instance.
     */
    @Getter
    private static AHBot instance;

    /**
     * The Hypixel API instance.
     */
    @Getter
    private final NewHypixelAPI hypixelAPI;

    /**
     * The JDA instance.
     */
    @Getter
    private final JDA jda;

    /**
     * The HTTP client.
     */
    @Getter
    private final ApacheHttpClient httpClient;

    /**
     * ID of notification channel.
     */
    private final String notificationChannelId = "1012361523047436419";

    private ArrayList<Auction> cachedAuctions = null;

    private Timer timer = new Timer();

    /**
     * Starts the bot.
     @param apiKey The Hypixel API key.
     @param botToken The Discord token.
     */
    private AHBot(String apiKey, String botToken) throws LoginException {
        httpClient = new ApacheHttpClient(UUID.fromString(apiKey));
        hypixelAPI = new NewHypixelAPI(httpClient);
        jda = JDABuilder.createDefault(botToken)
                .setEnabledIntents(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setActivity(Activity.playing("Hypixel Skyblock"))
                .build();
        instance = this;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    instance.setupOperations();
                }catch (Exception ignored){}
            }
        }, 0, 10000);
    }

    private void setupOperations() {
        var auctions = hypixelAPI.getSkyBlockAuctionByProfile("694236143d514479b9ab2fd5c36ae723");
        var soldAuctionsUuids = new ArrayList<String>();

        if(cachedAuctions != null) {
            for(var i=0; i<auctions.size(); i++){
                var finalI = i;

                if(cachedAuctions.stream().noneMatch(auction -> Objects.equals(auction.getUuid(), auctions.get(finalI).getUuid()))){
                    var embed = AhBotEmbedFactory.createEmbed("New Auction", "Item Name: "+auctions.get(finalI).getItemName(),
                            Map.of(
                                    "Price", auctions.get(finalI).getStartingBid()
                            ));
                    jda.getTextChannelById(notificationChannelId).sendMessageEmbeds(embed).queue();
                }

                if(cachedAuctions.stream().anyMatch(auction -> Objects.equals(auction.getUuid(), auctions.get(finalI).getUuid()))){
                    var cachedAction = cachedAuctions.stream().filter(auction -> Objects.equals(auction.getUuid(), auctions.get(finalI).getUuid())).findFirst().get();
                    if(cachedAction.getClaimedBidders().length<1 && auctions.get(finalI).getClaimedBidders().length>0){
                        soldAuctionsUuids.add(auctions.get(finalI).getUuid());
                    }
                }
            }

            for(var i=0; i<cachedAuctions.size(); i++){
                var finalI = i;

                if(auctions.stream().noneMatch(auction -> Objects.equals(auction.getUuid(), cachedAuctions.get(finalI).getUuid()))){
                    var embed = AhBotEmbedFactory.createEmbed("Auction Claimed or Removed", "Item Name: "+cachedAuctions.get(finalI).getItemName(),
                            Map.of(
                                    "Price", cachedAuctions.get(finalI).getStartingBid()
                            ));
                    jda.getTextChannelById(notificationChannelId).sendMessageEmbeds(embed).queue();
                }
            }

            soldAuctionsUuids.forEach(soldActionUuid -> {
                var auction = cachedAuctions.stream().filter(cachedAuction -> Objects.equals(cachedAuction.getUuid(), soldActionUuid)).findFirst().get();
                var embed = AhBotEmbedFactory.createEmbed("Auction Bought", "Item Name: "+auction.getItemName(), Map.of(
                        "Price", auction.getStartingBid()
                ));
                jda.getTextChannelById(notificationChannelId).sendMessageEmbeds(embed).queue();
            });
        }
        cachedAuctions = new ArrayList<>(auctions);
    }

    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Usage: java -jar ahbot.jar <apiKey> <botToken>");
            System.exit(1);
        }
        try{
            instance = new AHBot(args[0], args[1]);
        }catch (LoginException e){
            LOGGER.error("Failed to login to Discord", e);
            System.exit(1);
        }
    }
}
