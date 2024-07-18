import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleBuilder;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.test.FhirResourceLoader;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AncDakTest {

    @Test
    public void testANCIND07IndicatorWithInMemoryDataRepository() {
        //measure and libraries
        Repository contentRepository;
        {
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of("measure"), false);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            for (IBaseResource resource : resourceLoader.getResources()) {
                if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null && resource.getIdElement().getIdPart().contains(("TX-PVLS"))) {
                    builder.addTransactionUpdateEntry(resource);
                }
            }
            contentRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }

        //valuesets
        Repository terminologyRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(Bundle.class, "anc-dak/terminology-bundle.json"));

        //observation, patient and encounter
        Repository dataRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(Bundle.class, "measure/Observation-ANCIND7-Bundle.json"));

        R4MeasureProcessor measureProcessor = new R4MeasureProcessor(Repositories.proxy(dataRepository, contentRepository, terminologyRepository), MeasureEvaluationOptions.defaultOptions());

        MeasureReport report = measureProcessor.evaluateMeasure(
                Eithers.for3(new CanonicalType("http://fhir.org/guides/who/anc-cds/Measure/ANCIND07"), null, null),
                "2018-01-01", "2030-12-31", "population", null, null, null);

        System.out.println(FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
        //assertEquals(report.getGroupFirstRep().getMeasureScore().getValue().toString(), "0.6666666666666666");
    }

    private <T extends IBaseResource> T getResourceFromClasspath(Class<T> type, String location) {
        URL resource = getClass().getResource(location);
        if (resource != null) {
            try {
                IParser parser = FhirContext.forR4Cached().newJsonParser();
                return parser.parseResource(type, IOUtils.toString(resource, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

}
