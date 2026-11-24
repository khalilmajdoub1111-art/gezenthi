package tn.gezenthi.catalog;

/** Validation rules shared by several DTOs (kept identical to the admin form). */
public final class Validation {

    /** Image: https link, or a photo uploaded from the admin (data URL) — prototype until file storage exists. */
    public static final String IMAGE_URL = "^(https://|data:image/(jpeg|png|webp);base64,).*";
    public static final String IMAGE_MSG = "doit être un lien https:// ou une image importée";
    public static final int IMAGE_MAX = 3_000_000;

    public static final String BOARD_TYPES =
            "Tout compris|All inclusive soft|Demi-pension|Petit-déjeuner|Logement seul";
    public static final String SERVICE_KINDS = "Visa|Ferry CTN|TRE";

    private Validation() {
    }
}
