package ru.mpei.cimmaintainer.converter;

import org.apache.log4j.PropertyConfigurator;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import lombok.Getter;
import lombok.Setter;
import ru.mpei.cimmaintainer.JO.Terminal;

@Getter @Setter
public class SldFromCimBuilder {

    public static void sparQL(Model model) {
        String oneTab = "    ";
        String twoTabs = "        ";
        String threeTabs = "            ";

        Terminal terminal = new Terminal();

        String log4jConfPath = "src/test/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        Repository repository = new SailRepository(new MemoryStore());
        RepositoryConnection connection = repository.getConnection();
        connection.add(model);

        String queryString = "PREFIX cim: <" + "http://iec.ch/TC57/2013/CIM-schema-cim16#" + "> " +
                "SELECT ?tId ?name ?cnId ?ceId " +
                "WHERE { " +
                " ?t a cim:Terminal ; " +
                " cim:IdentifiedObject.mRID ?tId ; " +
                " cim:IdentifiedObject.name ?name ; " +
                " cim:Terminal.ConnectivityNode ?cn ; " +
                " cim:Terminal.ConductingEquipment ?ce . " +
                " ?ce cim:IdentifiedObject.mRID ?ceId ." +
                " ?cn cim:IdentifiedObject.mRID ?cnId ." +
                "}";

        TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

        try (TupleQueryResult result = query.evaluate()) {
            System.out.println("{");
            System.out.println(oneTab + "\"links\":\n    [");
            for (BindingSet solution : result) {
                System.out.println(twoTabs + "{");
                String tId = solution.getValue("tId").stringValue();
                String cnId = solution.getValue("cnId").stringValue();
                String ceId = solution.getValue("ceId").stringValue();
                String name = solution.getValue("name").stringValue();
                /*cnId - id, if name connectivity -cnId - random*/
                System.out.println(threeTabs + "\"id\": \"" + cnId + "\",");
                /*ceId - targetId/sourceId, if name connectivity - sourceId*/
                System.out.println(threeTabs + "\"sourceId\": \"" + ceId + "\",");
                /*tId - targetPortId, if name TA/DIS tId - sourcePortId*/
                System.out.println(threeTabs + "\"targetPortId\": \"" + tId + "\",");
                System.out.println(threeTabs + "\"name\": \"" + name + "\"");

                if (!result.hasNext()) {
                    System.out.println(twoTabs + "}");
                } else {
                    System.out.println(twoTabs + "},");
                }
            }
            System.out.println(oneTab + "]\n}");
        }
    }
}
