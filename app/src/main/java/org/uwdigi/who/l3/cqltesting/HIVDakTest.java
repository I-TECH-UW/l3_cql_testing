package org.uwdigi.who.l3.cqltesting;



import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.util.BundleBuilder;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.junit.Test;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.test.FhirResourceLoader;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.opencds.cqf.fhir.utility.repository.Repositories;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class HIVDakTest {

    private static InputStream open(String asset) {
        return PlanDefinition.class.getResourceAsStream(asset);
    }

    public static String load(InputStream asset) throws IOException {
        return new String(asset.readAllBytes(), StandardCharsets.UTF_8);
    }

    public static String load(String asset) throws IOException {
        return load(open(asset));
    }

    @Test
    public void testANCIND07IndicatorWithInMemoryDataRepository() {
        //measure and libraries
        Repository contentRepository;
        {
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of("smart-hiv"),false);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            for (IBaseResource resource : resourceLoader.getResources()) {
                if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null && resource.getIdElement().getIdPart().contains(("HIVIND19"))) {
                    builder.addTransactionUpdateEntry(resource);
                }
            }
            contentRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }

        //valuesets
//        Repository terminologyRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(Bundle.class, "anc-dak/terminology-bundle.json"));

        //observation, patient and encounter
        Repository dataRepository = new InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(Bundle.class, "smart-hiv/HIV.IND.19_bundle_5.json"));

        R4MeasureProcessor measureProcessor = new
                R4MeasureProcessor(Repositories.proxy(dataRepository, contentRepository, null), MeasureEvaluationOptions.defaultOptions());

        MeasureReport report = measureProcessor.evaluateMeasure(
                Eithers.for3(new CanonicalType("http://smart.who.int/HIV/Measure/HIVIND19"), null, null),
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
                throw new APIException("",e);
            }
        }

        return null;
    }
}