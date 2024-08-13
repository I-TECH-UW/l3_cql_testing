package org.uwdigi.who.l3.cqltesting.repository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.util.BundleBuilder;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.uwdigi.who.l3.cqltesting.matcher.L3ResourceMatcherR4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class L3InMemoryFhirRepository extends InMemoryFhirRepository {

    private final Map<String, Map<IIdType, IBaseResource>> resourceMap;

    private Repository repositoryProxy = null;

    public L3InMemoryFhirRepository(FhirContext context) {
        super(context);
        this.resourceMap = new HashMap<>();
    }

    public L3InMemoryFhirRepository(FhirContext context, IBaseBundle bundle) {
        super(context, bundle);
        var resources = BundleUtil.toListOfResources(context, bundle);
        this.resourceMap = resources.stream()
                .collect(Collectors.groupingBy(
                        IBaseResource::fhirType,
                        Collectors.toMap(r -> r.getIdElement().toUnqualifiedVersionless(), Function.identity())));
    }

    @Override
    public <B extends IBaseBundle, T extends IBaseResource> B search(Class<B> bundleType, Class<T> resourceType, Map<String, List<IQueryParameterType>> searchParameters, Map<String, String> headers) {
        BundleBuilder builder = new BundleBuilder(fhirContext());
        var resourceIdMap = resourceMap.computeIfAbsent(resourceType.getSimpleName(), r -> new HashMap<>());

        if (searchParameters == null || searchParameters.isEmpty()) {
            resourceIdMap.values().forEach(builder::addCollectionEntry);
            builder.setType("searchset");
            return (B) builder.getBundle();
        }

        Collection<IBaseResource> candidates;
        if (searchParameters.containsKey("_id")) {
            // We are consuming the _id parameter in this if statement
            var idQueries = searchParameters.get("_id");
            searchParameters.remove("_id");

            // The _id param can be a list of ids
            var idResources = new ArrayList<IBaseResource>(idQueries.size());
            for (var idQuery : idQueries) {
                var idToken = (TokenParam) idQuery;
                // Need to construct the equivalent "UnqualifiedVersionless" id that the map is
                // indexed by. If an id has a version it won't match. Need apples-to-apples Ids types
                var id = Ids.newId(fhirContext(), resourceType.getSimpleName(), idToken.getValue());
                var r = resourceIdMap.get(id);
                if (r != null) {
                    idResources.add(r);
                }
            }

            candidates = idResources;
        } else {
            candidates = resourceIdMap.values();
        }

        // Apply the rest of the filters
        var resourceMatcher = new L3ResourceMatcherR4(repositoryProxy != null ? repositoryProxy : this);
        for (var resource : candidates) {
            boolean include = true;
            for (var nextEntry : searchParameters.entrySet()) {
                var paramName = nextEntry.getKey();
                if (!resourceMatcher.matches(paramName, nextEntry.getValue(), resource)) {
                    include = false;
                    break;
                }
            }

            if (include) {
                builder.addCollectionEntry(resource);
            }
        }

        builder.setType("searchset");
        return (B) builder.getBundle();
    }

    public Repository getRepositoryProxy() {
        return repositoryProxy;
    }

    public void setRepositoryProxy(Repository repositoryProxy) {
        this.repositoryProxy = repositoryProxy;
    }
}
