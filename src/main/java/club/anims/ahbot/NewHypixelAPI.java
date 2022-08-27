package club.anims.ahbot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.http.HTTPQueryParams;
import net.hypixel.api.http.HypixelHttpClient;

import java.util.Arrays;
import java.util.List;

public class NewHypixelAPI extends HypixelAPI {
    private static final String BASE_URL = "https://api.hypixel.net/";

    private HypixelHttpClient httpClient;

    /**
     * @param httpClient a {@link HypixelHttpClient} that implements the HTTP behaviour for communicating with the API
     */
    public NewHypixelAPI(HypixelHttpClient httpClient) {
        super(httpClient);
        this.httpClient = httpClient;
    }

    public List<Auction> getSkyBlockAuctionByProfile(String profile) {
        var gson = new Gson();

        var url = BASE_URL + "skyblock/auction";
        var params = HTTPQueryParams.create()
                .add("profile", profile);
        url = params.getAsQueryString(url);
        var json = gson.fromJson(httpClient.makeAuthenticatedRequest(url).join().getBody(), JsonObject.class);
        var auctions = json.getAsJsonArray("auctions");
        var auctionsList = gson.fromJson(auctions, Auction[].class);
        return Arrays.stream(auctionsList).toList();
    }
}
