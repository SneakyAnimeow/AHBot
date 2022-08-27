package club.anims.ahbot;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Auction {
    @SerializedName("uuid")
    private String uuid;

    @SerializedName("item_name")
    private String itemName;

    @SerializedName("starting_bid")
    private String startingBid;

    @SerializedName("claimed_bidders")
    private String[] claimedBidders;

    @SerializedName("highest_bid_amount")
    private String highestBidAmount;
}
