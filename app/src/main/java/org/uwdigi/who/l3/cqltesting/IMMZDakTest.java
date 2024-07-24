package org.uwdigi.who.l3.cqltesting;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
import org.uwdigi.who.l3.cqltesting.repository.L3InMemoryFhirRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IMMZDakTest {

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
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of("immz"), true);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            for (IBaseResource resource : resourceLoader.getResources()) {
                builder.addTransactionUpdateEntry(resource);
            }
            contentRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }


        Repository terminologyRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath(Bundle.class, "immz/valueSets/terminology-bundle.json"));

        Repository dataRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), getResourceFromClasspath
                (Bundle.class, "immz/Immunization-Immunization1.json"));

        R4MeasureProcessor measureProcessor = new
                R4MeasureProcessor(Repositories.proxy(dataRepository, contentRepository, terminologyRepository), MeasureEvaluationOptions.defaultOptions());

        MeasureReport report = measureProcessor.evaluateMeasure(
                Eithers.for3(new CanonicalType("http://smart.who.int/immunizations-measles/Measure/IMMZIND12"), null, null),
                "2020-01-02", "2020-09-01", "population", null, null, null);

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
                throw new APIException("", e);
            }
        }

        return null;
    }
}