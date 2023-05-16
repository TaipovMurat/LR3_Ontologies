package ru.mpei.cimmaintainer.owl;

import org.apache.jena.ontology.*;
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
                ),
                new StationObject("ЛЭП",
                        List.of(
                                "lineVoltage",
                                "protection"
                        ),
                        List.of("have")
                ),
                List.of(
                        new Protection("ДЗЛ", List.of("installed_in")),
                        new Protection("ДЗ", List.of("installed_in")),
                        new Protection("ТЗНП", List.of("installed_in")),
                        new Protection("МТЗ", List.of("installed_in"))
                ),
                new StationObject("Выключатель",
                        List.of(
                                "breakerVoltage",
                                "protection"
                        ),
                        List.of("have")
                ),
                List.of(
                        new Protection("МТЗ_С_ПОН", List.of("installed_in")),
                        new Protection("МТЗ", List.of("installed_in")),
                        new Protection("АВР", List.of("installed_in")),
                        new Protection("ТО", List.of("installed_in")),
                        new Protection("ЗОЗЗ", List.of("installed_in")),
                        new Protection("ДЗ", List.of("installed_in"))
                ),
                new StationObject("Ошиновка",
                        List.of(
                                "isReactor",
                                "protection"
                        ),
                        List.of("have")
                ),
                List.of(
                        new Protection("ДЗО", List.of("installed_in"))
                )
        );

    }

    private static Map<String, String> transfNames() {
        return Map.of(
                "ТДЦ-150000_220/110", "ИЭУ-1",
                "ДЦ-63000_110/10", "ИЭУ-2",
                "ТДЦ-160000_330/220", "ИЭУ-4"
        );
    }
    private static Map<String, String> lineNames() {
        return Map.of(
                "ЛЭП-1_110", "ИЭУ-3"
        );
    }
    private static Map<String, String> breakerNames() {
        return Map.of(
                "BRK-1_6", "ИЭУ-6",
                "BRK-2_35", "ИЭУ-7",
                "BRK-3_35", "ИЭУ-8"
        );
    }
    private static Map<String, String> busNames() {
        return Map.of(
                "BUS-1_1", "ИЭУ-9"
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
                    Map<String, String> names = transfNames();

                    for (Map.Entry<String, String> name : names.entrySet()) {
                        Individual transfIndividual = object.getKey().createIndividual(nameSpace + name.getKey());
                        System.out.println(object.getKey().getLocalName());
                        transformerIndividuals.add(transfIndividual);
                        for (OntClass protection : object.getValue()) {
                            Individual protIndividual = protection.createIndividual(nameSpace + name.getValue());
                            protectionIndividuals.add(protIndividual);
                        }
                    }
                    objectIndividuals.put(transformerIndividuals, protectionIndividuals);
                    break;

                case "ЛЭП":
                    List<Individual> lineIndividuals = new ArrayList<>();
                    List<Individual> protectionlineIndividuals = new ArrayList<>();
                    Map<String, String> lineNames = lineNames();

                    for (Map.Entry<String, String> name : lineNames.entrySet()) {
                        Individual lineIndividual = object.getKey().createIndividual(nameSpace + name.getKey());

                        lineIndividuals.add(lineIndividual);
                        for (OntClass protection : object.getValue()) {
                            Individual protIndividual = protection.createIndividual(nameSpace + name.getValue());
                            protectionlineIndividuals.add(protIndividual);
                        }
                    }

                    objectIndividuals.put(lineIndividuals, protectionlineIndividuals);
                    break;
                case "Выключатель":
                    List<Individual> brkIndividuals = new ArrayList<>();
                    List<Individual> protectionBRKIndividuals = new ArrayList<>();
                    Map<String, String> brkNames = breakerNames();

                    for (Map.Entry<String, String> name : brkNames.entrySet()) {
                        Individual lineIndividual = object.getKey().createIndividual(nameSpace + name.getKey());

                        brkIndividuals.add(lineIndividual);
                        for (OntClass protection : object.getValue()) {
                            Individual protIndividual = protection.createIndividual(nameSpace + name.getValue());
                            protectionBRKIndividuals.add(protIndividual);
                        }
                    }

                    objectIndividuals.put(brkIndividuals, protectionBRKIndividuals);
                   break;
                case "Ошиновка":
                    List<Individual> busIndividuals = new ArrayList<>();
                    List<Individual> protectionBUSIndividuals = new ArrayList<>();
                    Map<String, String> busNames = busNames();

                    for (Map.Entry<String, String> name : busNames.entrySet()) {
                        Individual lineIndividual = object.getKey().createIndividual(nameSpace + name.getKey());

                        busIndividuals.add(lineIndividual);
                        for (OntClass protection : object.getValue()) {
                            Individual protIndividual = protection.createIndividual(nameSpace + name.getValue());
                            protectionBUSIndividuals.add(protIndividual);
                        }
                    }

                    objectIndividuals.put(busIndividuals, protectionBUSIndividuals);
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
                    objInd.addProperty(winding, "3");
                }
                DatatypeProperty power = (DatatypeProperty) properties.get(0).get("power");
                DatatypeProperty voltage = (DatatypeProperty) properties.get(0).get("voltage");
                DatatypeProperty winding = (DatatypeProperty) properties.get(0).get("winding");
                if (objInd.getProperty(power) != null && objInd.getProperty(power).getDouble() >= 160_000
                        && (objInd.getProperty(voltage).getDouble() >= 110)) {
                    DatatypeProperty protection1 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection1, "Первый_комплект_ДЗТ");
                    DatatypeProperty protection2 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection2, "Второй_комплект_ДЗТ");
                } else if (objInd.getProperty(power) != null && objInd.getProperty(power).getDouble() < 160_000
                        && (objInd.getProperty(voltage).getDouble() > 35 && objInd.getProperty(voltage).getDouble() < 110))
                {
                    DatatypeProperty protection = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection,"Один_комплект_ДЗТ");
                }
                if (objInd.getProperty(power) != null && objInd.getProperty(winding).getDouble() >= 3 ) {
                    DatatypeProperty protection = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection,"Защита от перегрузки");
                }
                if ("ЛЭП".equals(objInd.getOntClass().getLocalName())) {
                    String lineVoltageStr = objInd.getURI().split("#")[1].split("_")[1].split("/")[0];
                    DatatypeProperty lineVoltage = (DatatypeProperty) properties.get(0).get("lineVoltage");
                    objInd.addProperty(lineVoltage, lineVoltageStr);
                }
                DatatypeProperty lineVoltage = (DatatypeProperty) properties.get(0).get("lineVoltage");
                if (objInd.getProperty(lineVoltage) != null && objInd.getProperty(lineVoltage).getDouble() >= 110
                        && objInd.getProperty(lineVoltage).getDouble() <= 220) {
                    DatatypeProperty protection1 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection1, "ДЗЛ");
                    DatatypeProperty protection2 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection2, "ДЗ");
                    DatatypeProperty protection3 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection3,"ТЗНП");
                    DatatypeProperty protection4 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection4,"АПВ");
                    DatatypeProperty protection5 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection5,"ТО");
                    DatatypeProperty protection6 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection6,"МТЗ");
                }
                if ("Выключатель".equals(objInd.getOntClass().getLocalName())) {
                    String breakerVoltageStr = objInd.getURI().split("#")[1].split("_")[1].split("/")[0];
                    DatatypeProperty breakerVoltage = (DatatypeProperty) properties.get(0).get("breakerVoltage");
                    objInd.addProperty(breakerVoltage, breakerVoltageStr);
                }
                DatatypeProperty breakerVoltage = (DatatypeProperty) properties.get(0).get("breakerVoltage");
                if (objInd.getProperty(breakerVoltage) != null && objInd.getProperty(breakerVoltage).getDouble() >= 6
                        && objInd.getProperty(breakerVoltage).getDouble() <= 35) {
                    DatatypeProperty protection1 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection1, "МТЗ");
                }
                if ("Ошиновка".equals(objInd.getOntClass().getLocalName())) {
                    String numberOfReactorsStr = objInd.getURI().split("#")[1].split("_")[1].split("/")[0];
                    DatatypeProperty numberOfReactors = (DatatypeProperty) properties.get(0).get("isReactor");
                    objInd.addProperty(numberOfReactors, numberOfReactorsStr);
                }
                DatatypeProperty numberOfReactors = (DatatypeProperty) properties.get(0).get("isReactor");
                if (objInd.getProperty(numberOfReactors) != null &&
                        objInd.getProperty(numberOfReactors).getDouble() >= 1) {
                    DatatypeProperty protection1 = (DatatypeProperty) properties.get(0).get("protection");
                    objInd.addProperty(protection1, "ДЗО");
                }
            }
        }


    }
}
