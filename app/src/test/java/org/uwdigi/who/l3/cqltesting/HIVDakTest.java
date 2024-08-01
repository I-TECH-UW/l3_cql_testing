package org.uwdigi.who.l3.cqltesting;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.util.BundleBuilder;
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
import org.opencds.cqf.fhir.utility.repository.Repositories;
import org.uwdigi.who.l3.cqltesting.repository.L3InMemoryFhirRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

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
    public void testHIVIND27() {
        //measure, libraries, plandefinitions, and activitydefinitions
        L3InMemoryFhirRepository contentRepository;
        {
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of(Paths.get("smart-hiv", "content").toString()), true);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            processResourcesToBundle(builder, resourceLoader.getResources());
            contentRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }

        // valuesets, codesystems, and codemaps
        L3InMemoryFhirRepository terminologyRepository;
        {
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of(Paths.get("smart-hiv", "terminology").toString()), true);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            processResourcesToBundle(builder, resourceLoader.getResources());
            terminologyRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }

        // anything else
        L3InMemoryFhirRepository dataRepository;
        {
            FhirResourceLoader resourceLoader = new FhirResourceLoader(FhirContext.forR4Cached(), getClass(), List.of(Paths.get("smart-hiv", "HIV.IND.27").toString()), true);
            BundleBuilder builder = new BundleBuilder(FhirContext.forR4Cached());
            processResourcesToBundle(builder, resourceLoader.getResources());
            dataRepository = new L3InMemoryFhirRepository(FhirContext.forR4Cached(), builder.getBundle());
        }

        Repository repository = Repositories.proxy(dataRepository, contentRepository, terminologyRepository);

        contentRepository.setRepositoryProxy(repository);
        terminologyRepository.setRepositoryProxy(repository);
        dataRepository.setRepositoryProxy(repository);

        R4MeasureProcessor measureProcessor = new R4MeasureProcessor(repository, MeasureEvaluationOptions.defaultOptions());

        MeasureReport report = measureProcessor.evaluateMeasure(
                Eithers.forLeft3(new CanonicalType("http://smart.who.int/HIV/Measure/HIVIND27")),
                "2020-01-01", "2020-12-31", "continuous-variable", null, null, null);

        System.out.println(FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().setPrettyPrint(true).encodeResourceToString(report));
        //assertEquals(report.getGroupFirstRep().getMeasureScore().getValue().toString(), "0.6666666666666666");
    }

    private void processResourcesToBundle(BundleBuilder builder, List<IBaseResource> resources) {
        for (IBaseResource resource : resources) {
            if (resource instanceof Bundle) {
                processResourcesToBundle(builder, ((Bundle) resource).getEntry().stream().map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList()));
            }
            builder.addTransactionUpdateEntry(resource);
        }
    }
}