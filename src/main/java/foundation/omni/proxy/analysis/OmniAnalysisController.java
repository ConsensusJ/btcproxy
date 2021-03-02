package foundation.omni.proxy.analysis;

import foundation.omni.CurrencyID;
import foundation.omni.OmniValue;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.reactivex.rxjava3.core.Single;
import org.consensusj.analytics.service.RichListService;
import org.consensusj.analytics.service.TokenRichList;

/**
 * Simple controller to expose REST API for an Omni rich list.
 */
@Controller("/omni/analysis")
public class OmniAnalysisController {
    private final int richListSize = 12;

    private final RichListService<OmniValue, CurrencyID> richListService;

    public OmniAnalysisController(RichListService<OmniValue, CurrencyID> richListService, OmniAnalysisService omniAnalysisService) {
        this.richListService = richListService;
    }

    @Get(uri="/richlist/{currencyId}", produces = MediaType.APPLICATION_JSON)
    public Single<TokenRichList<OmniValue, CurrencyID>> richList(int currencyId) {
        return richListService.richList(CurrencyID.of(currencyId), richListSize);
    }
}
