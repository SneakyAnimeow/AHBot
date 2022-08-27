package club.anims.ahbot;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.hypixel.api.apache.ApacheHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.*;

public class AHBot {
    /**
     * The Factory of embeds.
     */
    public static class AHBotEmbedFactory {
        /**
         * Creates a new embed.
         * @param title The title of the embed.
         * @param description The description of the embed.
         * @param fields The fields of the embed.
         * @return New embed.
         */
        public static MessageEmbed createEmbed(String title, String description, @Nullable Collection<? extends MessageEmbed.Field> fields) {
            var embedBuilder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription(description)
                    .setColor(Color.CYAN)
                    .setThumbnail("https://hypixel.net/attachments/1621344745339-png.2560259");

            if(fields != null) {
                fields.forEach(embedBuilder::addField);
            }

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
    private final String notificationChannelId;

    /**
     * Timer for scheduling TimerTasks.
     */
    private final Timer timer = new Timer();

    /**
     * ID of SkyBlock profile.
     */
    private final String skyBlockProfile;

    /**
     * Message containing information about all auctions.
     */
    private Message auctionMessage;

    /**
     * Starts the bot.
     @param apiKey The Hypixel API key.
     @param botToken The Discord token.
     */
    private AHBot(String apiKey, String skyBlockProfile, String botToken, String notificationChannelId) throws LoginException {
        httpClient = new ApacheHttpClient(UUID.fromString(apiKey));
        hypixelAPI = new NewHypixelAPI(httpClient);
        jda = JDABuilder.createDefault(botToken)
                .setEnabledIntents(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .setActivity(Activity.playing("Hypixel SkyBlock"))
                .build();
        this.notificationChannelId = notificationChannelId;
        this.skyBlockProfile = skyBlockProfile;
        instance = this;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    instance.setupOperations();
                }catch (Exception e){
                    LOGGER.error("Error while setting up operations", e);
                }
            }
        }, 0, 10000);
    }

    private void setupOperations() {
        if(auctionMessage == null || Objects.requireNonNull(jda.getTextChannelById(notificationChannelId)).retrieveMessageById(auctionMessage.getId()).complete()== null) {
            auctionMessage = Objects.requireNonNull(jda.getTextChannelById(notificationChannelId))
                    .sendMessageEmbeds(AHBotEmbedFactory.createEmbed("Auction House", "Loading...", null)).complete();
        }

        var auctions = new ArrayList<>(hypixelAPI.getSkyBlockAuctionByProfile(skyBlockProfile));

        //phantom auction fix
        auctions.removeIf(auction -> auction.getUuid().equals("9b242e2aca794804a3da61a9c3671adb"));

        var fields = new ArrayList<MessageEmbed.Field>();

        var info = new StringBuilder();

        for(var i=0; i<auctions.size(); i++) {
            info.append(String.format("```Name: %s\nLowest Bid: %s\nStatus: %s\nHighest Bid: %s\n```", auctions.get(i).getItemName(), auctions.get(i).getStartingBid(),
                    auctions.get(i).getHighestBidAmount(),
                    (auctions.get(i).getClaimedBidders().length < 1 ? "Available" : "Sold")));

            if(i%2 == 0) {
                fields.add(new MessageEmbed.Field("", info.toString(), false));
                info = new StringBuilder();
            }
        }

        if(info.length() > 0) {
            fields.add(new MessageEmbed.Field("", info.toString(), false));
        }

        auctionMessage.editMessageEmbeds(AHBotEmbedFactory.createEmbed("Auction House",
                "Unsold Auctions: "+ auctions.stream().filter(auction -> auction.getClaimedBidders().length<1).count(),
                fields)).queue();
    }

    public static void main(String[] args) {
        if(args.length < 4){
            System.out.println("Usage: java -jar ahbot.jar <apiKey> <skyBlockProfile> <botToken> <notificationChannelId>");
            System.exit(1);
        }

        try{
            instance = new AHBot(args[0], args[1], args[2], args[3]);
        }catch (LoginException e){
            LOGGER.error("Failed to login to Discord", e);
            System.exit(1);
        }
    }
}
