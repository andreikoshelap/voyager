package ee.voyagelog.harbour;

import java.math.BigDecimal;

public record HarbourResponse(
        Long id,
        String name,
        Double lat,
        Double lon,
        String vhfChannel,
        String phone,
        BigDecimal depthM,
        String priceNote,
        boolean hasHost,
        String harbourCode,
        String address,
        String website,
        String email,
        String amenities,
        String sourceUrl) {

    public static HarbourResponse from(Harbour h) {
        return new HarbourResponse(
                h.getId(), h.getName(), h.getLat(), h.getLon(),
                h.getVhfChannel(), h.getPhone(), h.getDepthM(), h.getPriceNote(),
                h.getTelegramChatId() != null,
                h.getHarbourCode(), h.getAddress(), h.getWebsite(), h.getEmail(),
                h.getAmenities(), h.getSourceUrl());
    }
}
