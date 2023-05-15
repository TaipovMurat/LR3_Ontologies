package ru.mpei.cimmaintainer.owl;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.jena.ontology.OntModelSpec.OWL_MEM;

public class OntologyCreator {
    private static final String nameSpace ="http://mpei.ru/LR3#";

    public static void main(String[] args) {
        OntModel ontModel = createModel();
        Map<StationObject, List<Protection>> protections = createMap();

        Map<OntClass, List<OntClass>> createdClasses = createClasses(ontModel, protections);

        List<Map<String, ? extends OntProperty>> createdProperties = createProperties(ontModel, protections);

        Map<List<Individual>, List<Individual>> createdIndividuals = createIndividuals(createdClasses);

        setProperty(createdProperties, createdIndividuals);

        try {
            OutputStream outputStream = new FileOutputStream("src/test/resources/ontology.owl");
            ontModel.write(outputStream, "RDF/XML-ABBREV");
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<StationObject, List<Protection>> createMap() {
        return Map.of(
                new StationObject("Трансформатор",
                        List.of(
                                "power",
                                "voltage",
                                "mode",
                                "winding",
                                "protection"
                                ),
                        List.of("have")
                ),
                List.of(
                        new Protection("ДЗТ", List.of("installed_in")),
                        new Protection("Защита_от_перегрузки", List.of("installed_in")),
                        new Protection("МТЗ_ВН", List.of("installed_in")),
                        new Protection("ДЗ,_ТНЗНП", List.of("installed_in")),
                        new Protection("ТЗНП", List.of("installed_in"))
                )
        );
    }

    private static Map<String, String> objectsNames() {
        return Map.of(
                "ТДЦ-150000_220/110", "ИЭУ-1",
                "ДЦ-63000_110/10", "ИЭУ-2"
        );
    }

    private static OntModel createModel() {
        Model model = ModelFactory.createDefaultModel();
        return ModelFactory.createOntologyModel(OWL_MEM, model);
    }

    private static Map<OntClass, List<OntClass>> createClasses(
            OntModel ontModel,
            Map<StationObject, List<Protection>> protections) {
        Map<OntClass, List<OntClass>> classes = new HashMap<>();

        for (Map.Entry<StationObject, List<Protection>> object : protections.entrySet()) {
            OntClass ontClass = ontModel.createClass(nameSpace + object.getKey().getName());
            List<OntClass> subClassesList = new ArrayList<>();
            for (Protection protection : object.getValue()) {
                OntClass subClass = ontModel.createClass(nameSpace + protection.getName());
                ontClass.addSubClass(subClass);
                subClassesList.add(subClass);
            }
            classes.put(ontClass, subClassesList);
        }
        return classes;
    }

    /**
     *
     * @param ontModel
     * @param objects
     * @return
     * List[0] - Map<String, DatatypeProperty> datatypePropertyMap
     * List[1] - Map<String, ObjectProperty> objectPropertyMap
     */
    private static List<Map<String, ? extends OntProperty>> createProperties(
            OntModel ontModel,
            Map<StationObject, List<Protection>> objects) {
        Map<String, DatatypeProperty> datatypePropertyMap = new HashMap<>();
        Map<String, ObjectProperty> objectPropertyMap = new HashMap<>();
        for (Map.Entry<StationObject, List<Protection>> object : objects.entrySet()) {
            for (String dataProperty : object.getKey().getDataProperties()) {
                DatatypeProperty datatypeProperty = ontModel.createDatatypeProperty(nameSpace + dataProperty);
                datatypePropertyMap.put(dataProperty, datatypeProperty);
            }
            for (String objectProperty : object.getKey().getObjectProperties()) {
                ObjectProperty objProperty = ontModel.createObjectProperty(nameSpace + objectProperty);
                objectPropertyMap.put(objectProperty, objProperty);
            }
            for (Protection protection : object.getValue()) {
                for (String objectProperty : protection.getObjectProperties()) {
                    ObjectProperty protectionProperty = ontModel.createObjectProperty(nameSpace + objectProperty);
                    objectPropertyMap.put(objectProperty, protectionProperty);
                }
            }
        }
        return List.of(datatypePropertyMap, objectPropertyMap);
    }

    /**
     *
     * @param classes
     * @return
     * key List<Individual> - transformerIndividuals
     * value List<Individual> - protectionIndividuals
     */
    private static Map<List<Individual>, List<Individual>> createIndividuals(Map<OntClass, List<OntClass>> classes) {
        Map<List<Individual>, List<Individual>> objectIndividuals = new HashMap<>();
        for (Map.Entry<OntClass, List<OntClass>> object : classes.entrySet()) {
            switch (object.getKey().getLocalName()) {
                case "Трансформатор":
                    List<Individual> transformerIndividuals = new ArrayList<>();
                    List<Individual> protectionIndividuals = new ArrayList<>();
                    Map<String, String> names = objectsNames();

                    for (Map.Entry<String, String> name : names.entrySet()) {
                        Individual transfIndividual = object.getKey().createIndividual(nameSpace + name.getKey());
                        transformerIndividuals.add(transfIndividual);
                        for (OntClass protection : object.getValue()) {
                            Individual protIndividual = protection.createIndividual(nameSpace + name.getValue());
                            protectionIndividuals.add(protIndividual);
                        }
                    }
                    objectIndividuals.put(transformerIndividuals, protectionIndividuals);
                    break;
            }
        }
        return objectIndividuals;
    }

    public static void connectProperties(Individual individual1,
                                         Individual individual2,
                                         ObjectProperty objectProperty) {
        individual1.addProperty(objectProperty, individual2);
    }

    private static void setProperty(
            List<Map<String, ? extends OntProperty>> properties,
            Map<List<Individual>, List<Individual>> individuals) {
        for (Map.Entry<List<Individual>, List<Individual>> individualMap : individuals.entrySet()) {
            for (int i = 0, j = 0; i < individualMap.getKey().size(); i++, j += 5) {
                Individual objInd = individualMap.getKey().get(i);
                Individual protInd = individualMap.getValue().get(j);
                ObjectProperty have = (ObjectProperty) properties.get(1).get("have");
                ObjectProperty installed = (ObjectProperty) properties.get(1).get("installed_in");
                connectProperties(objInd, protInd, have);
                connectProperties(protInd, objInd, installed);

                if ("Трансформатор".equals(objInd.getOntClass().getLocalName())) {
//                    ТДЦ-150000_220/110
                    String powerStr = objInd.getURI().split("#")[1].split("-")[1].split("_")[0];
                    DatatypeProperty power = (DatatypeProperty) properties.get(0).get("power");
                    objInd.addProperty(power, powerStr);
                    String voltageStr = objInd.getURI().split("#")[1].split("_")[1].split("/")[0];
                    DatatypeProperty voltage = (DatatypeProperty) properties.get(0).get("voltage");
                    objInd.addProperty(voltage, voltageStr);
                    DatatypeProperty mode = (DatatypeProperty) properties.get(0).get("mode");
                    objInd.addProperty(mode, "single");
                    DatatypeProperty winding = (DatatypeProperty) properties.get(0).get("winding");
                    objInd.addProperty(winding, "2");
                }
                DatatypeProperty power = (DatatypeProperty) properties.get(0).get("power");
                if (objInd.getProperty(power) != null && objInd.getProperty(power).getDouble() < 160_000) {
                    DatatypeProperty protection = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection, "ДЗТ");
                }
            }
        }
    }

}
