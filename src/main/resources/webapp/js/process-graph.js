// Create a new directed graph
// Globale Variablen für das SVG-Element, den D3-Graph, das Graph-Element, den Zoom und den Renderer
// Variablen für das Datenmodell
var commits, revisions, referenceCommits, branches;


// Set up zoom support
var svg;
var inner;
 
var _svg, spinner;

var w;
function updateWindow(){
    _svg.attr("width", w.clientWidth);
}
window.onresize = updateWindow;

$(document).ready(function () {
	_svg = $("#svg");
	 w = $("#visualisation")[0];
	 updateWindow();
	// Loading Symbol in der Mitte des Graphen einfügen
	spinner = jQuery.parseHTML("<div style='position: absolute; top:" + (_svg.position().top + (_svg.attr('height') / 2) - 34) + "px;left:" + (_svg.position().left + (_svg.attr('width') / 2) - 34) + "px;'><i class='fa fa-spinner fa-pulse fa-3x'></i></div>");
	// Loading Symbol ins DOM einfügen
	_svg.after(spinner);
	
 
});

// Center the graph
var center = function () {
    // Verhältnis von Graph zur SVG Größe
    var widthscale = svg.attr("width") / (g.graph().width + 30);
    var heightscale = svg.attr("height") / (g.graph().height + 30);
    // Kleineres Verhältnis zur Anpassung auswählen
    var scale = widthscale < heightscale ? widthscale : heightscale;
    // Graphen nicht größer als doppelt so groß anzeigen
    scale = scale > 2 ? 2 : scale;
    // Graph über D3 mittig positionieren und skalieren
    zoom
        .translate([(svg.attr("width") - g.graph().width * scale) / 2, (svg.attr("height") - g.graph().height * scale) / 2])
        .scale(scale)
        .event(svg);
    // Loader ausblenden
    $(spinner).hide();
};


function padStr(i) {
    return (i < 10) ? "0" + i : "" + i;
}

////Funktion, die die Tags als Knoten mit Kanten erstellt
//var createTags = function () {
//    // Über alle Tags iterieren
//    Object.keys(referenceCommits).forEach(function (referenceCommit) {
//        // Alle Daten können im D3 Modell hinterlegt werden
//        var value = referenceCommits[referenceCommit];
//        // Konfigurationsparameter setzen
//        // Anzeigename
//        value.label = referenceCommits[referenceCommit].title;
//        // Höhe
//        value.height = 5;
//        // Hellblaue Rahmenfarbe
//        value.style = "stroke:#60b1fc;";
//        // Schriftgröße
//        value.labelStyle = "font-size:9px;";
//        
//        value.shape = "ellipse";
//        // Knoten erzeugen
//        g.setNode(referenceCommit, value);
//        // Für jede Revision, die der Tag referenziert, muss eine Kante erstellt werden
//        referenceCommits[referenceCommit].used.forEach(function (v) {
//            // Kante von der Revision zum Tag erstellen
//            g.setEdge(v, referenceCommit, {
//                //Hellblaue, gestrichelte Linie definieren
//                style: "stroke:#60b1fc;fill:none;stroke-dasharray: 5, 5;",
//                // Hellblauen Pfeil definieren
//                arrowheadStyle: "fill:#60b1fc;stroke:#60b1fc;",
//                // Rundere Linien
//                lineInterpolate: "basis",
//                // Mindestabstand 2
//                minlen: 2
//            });
//        });
//    });
//};

//Funktion, die die Tags als Knoten mit Kanten erstellt
var createTags = function () {
    // Über alle Tags iterieren
    Object.keys(referenceCommits).forEach(function (referenceCommit) {
        // Alle Daten können im D3 Modell hinterlegt werden
        var value = referenceCommits[referenceCommit];
        // Konfigurationsparameter setzen
        // Anzeigename
        value.label = referenceCommits[referenceCommit].title;
        // Höhe
        value.height = 5;
        // Hellblaue Rahmenfarbe
        value.style = "stroke:#60b1fc;";
        // Schriftgröße
        value.labelStyle = "font-size:9px;";
        // Knoten erzeugen
        g.setNode(referenceCommit, value);
        // Für jede Revision, die der Tag referenziert, muss eine Kante erstellt werden
        referenceCommits[referenceCommit].used.forEach(function (v) {
            // Kante von der Revision zum Tag erstellen
            g.setEdge(v, referenceCommit, {
                //Hellblaue, gestrichelte Linie definieren
                style: "stroke:#60b1fc;fill:none;stroke-dasharray: 5, 5;",
                // Hellblauen Pfeil definieren
                arrowheadStyle: "fill:#60b1fc;stroke:#60b1fc;",
                // Rundere Linien
                lineInterpolate: "basis",
                // Mindestabstand 2
                minlen: 2
            });
        });
    });
};

// Funktion, die die Branches als Knoten mit Kanten erstellt
var createBranches = function () {
    // Über alle Branches iterieren
    Object.keys(branches).forEach(function (branch) {
        // Alle Daten können im D3 Modell hinterlegt werden
        var value = branches[branch];
        // Konfigurationsparameter setzen
        // Anzeigename
        value.label = branches[branch].label;
        // Höhe
        value.height = 5;
        // In der Farbe des Branches füllen
        value.style = "fill:" + branches[branch].color + ";stroke:#fff;";
        // Textgröße
        value.labelStyle = "font-size:9px;";
        // Node erstellen
        g.setNode(branch, value);
        // Zugehörige Linie zur Revision erstellen
        g.setEdge(branch, branches[branch].head, {
            // Mindestabstand 1
            minlen: 1,
            // Linie ohne Pfeil
            arrowhead: "undirected",
            // Linie in der Farbe des Branches
            style: "stroke:" + branches[branch].color + ";fill:none;",
            arrowheadStyle: "fill:" + branches[branch].color + ";stroke:" + branches[branch].color + ";",
            // Rundere Linien
            lineInterpolate: "basis"
        });
    });
};


//Funktion um Tags einzublenden
var showTags = function () {
    // Loader einblenden
    $(spinner).show();
    // Tag-Knoten und Kanten erstellen
    createTags();
    // Graph rendern und ins DOM einbinden
    render(inner, g);
    // tipsy-Funktion an die eingebundenen Knoten binden
//  bindTags();
    // Graph neu skalieren und positionieren
    center();
    //color change on click
    d3.selectAll("g.node rect").on("click", function(d) {
        var c = d3.select(this).classed("doubled");
        d3.select(this).classed("doubled", !c);
    });
};

// Funktion um Tags auszublenden
var hideTags = function () {
    // Loader einblenden
    $(spinner).show();
    // Alle Knoten mit D3 erfassen, Tags herausfiltern und löschen
    inner.selectAll("g.node").filter(function (v) {
        return referenceCommits[v] != null;
    })
        .each(function (v) {
            g.removeNode(v);
        });
    // Graph ohne Tags neu rendern
    render(inner, g);
    // Graph neu skalieren und positionieren
    center();
    //color change on click
    d3.selectAll("g.node rect").on("click", function(d) {
        var c = d3.select(this).classed("doubled");
        d3.select(this).classed("doubled", !c);
    });
};

// Funktion um Branches einzubinden
var showBranches = function () {
    // Loader einbinden
    $(spinner).show();
    // Branch-Knoten und Kanten erstellen
    createBranches();
    // Graph rendern und in DOM einbinden
    render(inner, g);
    // Graph neu skalieren und positionieren
    center();
    //color change on click
    d3.selectAll("g.node rect").on("click", function(d) {
        var c = d3.select(this).classed("doubled");
        d3.select(this).classed("doubled", !c);
    });
};

// Funktion um Branches auszublenden
var hideBranches = function () {
    // Loader einblenden
    $(spinner).show();
    // Alle Knoten mit D3 erfassen, Branches herausfiltern und löschen
    inner.selectAll("g.node").filter(function (v) {
        return branches[v] != null;
    })
        .each(function (v) {
            g.removeNode(v);
        });
    // Graph ohne Branches neu rendern
    render(inner, g);
    // Graph neu skalieren und positionieren
    center();
    d3.selectAll("g.node rect").on("click", function(d) {
        var c = d3.select(this).classed("doubled");
        d3.select(this).classed("doubled", !c);
    });
};
/** Hauptfunktion, um kompletten Graphen zu erstellen.
 *
 *  _JSON: URL zu JSON-Daten oder Daten selbst
 *  _showBranches: true, wenn Branches mit angezeigt werden sollen (Optional)
 *  _showTags: true, wenn Tags mit angezeigt werden sollen (Optional)
 */
var drawGraph = function (_JSON, _showBranches, _showTags) {
    // Falls _showBranches nicht übergeben wurde, standardmäßig auf false setzen
    _showBranches = _showBranches || false;
    // Falls _showTags nicht übergeben wurden, standardmäßig auf false setzen
    _showTags = _showTags || false;
    // Loader einblenden
    $(spinner).show();
    // Loader einblenden
    $.getJSON(_JSON, function (data, status) {
        // Prüfen, ob die Daten erfolgreich geladen wurden
        if (status != "success") {
            alert("Error getting JSON Data: Status " + status);
        }
        // Farbpalette für die Branches aus D3-Funktion
        var colors = d3.scale.category10();
        // Zähler initialisieren
        var j = 1, k = 0;
        // D3 Graph neu erstellen
        g = new dagreD3.graphlib.Graph();
        g.setGraph({});
        g.setDefaultEdgeLabel(function () {
            return {};
        });
        // Falls es bereits einen Graph gab, diesen löschen
       
        svg = d3.select("#svg");
        // Das Graph-Element in das SVG-Element einfügen
        inner = svg.select("g");
        // Datenvariablen initialisieren
        commits = {};
        revisions = {};
        referenceCommits = {};
        branches = {};
        // Hellblau als Farbe verbrauchen, die Farbe geht an die Tags
        colors(0);
        // Über alle Eingangsdaten iterieren (Key-Value-Paare)
        $.each(data, function (key, value) {
            // Array der Datenobjekte mit den Typen laden
            var types = value["http://www.w3.org/1999/02/22-rdf-syntax-ns#type"];
            // Zähler für die Farbzuweisung
            j++;
            // Über alle verschiedenen Typen iterieren und je nach Typ behandeln
            for (var i = 0; i < types.length; i++) {
                switch (types[i].value) {
                    // Falls Commit
                    case "http://eatld.et.tu-dresden.de/rmo#Commit":
                        // Commit intialisieren
                        commits[key] = {};
                        // Nachricht setzen
                        commits[key].title = value["http://purl.org/dc/terms/title"][0].value;
                        // ID des Erstellers hinterlegen
                        commits[key].wasAssociatedWith = value["http://www.w3.org/ns/prov#wasAssociatedWith"][0].value;
                        // ID der Revision, die der Commit erstellt hat setzen
                        commits[key].generated = value["http://www.w3.org/ns/prov#generated"][0].value;
                        // Alle IDs der Revisionen, die in den Commit eingeflossen sind setzen (length > 1 --> Merge)
                        commits[key].used = [];
                        for (k = 0; k < value["http://www.w3.org/ns/prov#used"].length; k++) {
                            commits[key].used.push(value["http://www.w3.org/ns/prov#used"][k].value);
                        }
                        // Zeitstempel, der für den Commit hinterlegt sind setzen
                        commits[key].time = value["http://www.w3.org/ns/prov#atTime"][0].value;
                        // Commit in der Revision hinterlegen, die aus dem Commit hervorging. Falls es die Revision im Datenmodell noch nicht gibt muss sie erstellt werden
                        if (revisions[commits[key].generated] == null) {
                            revisions[commits[key].generated] = {};
                        }
                        revisions[commits[key].generated].commit = key;
                        break;
                    // Falls Revision
                    case "http://eatld.et.tu-dresden.de/rmo#Revision":
                        // Falls die Revision noch nicht im Datenmodell existiert muss sie initialisiert werden
                        if (revisions[key] == null) {
                            revisions[key] = {};
                        }
                        // ID der entfernten Daten setzen
                        revisions[key].deltaRemoved = value["http://eatld.et.tu-dresden.de/rmo#deltaRemoved"][0].value;
                        // ID der hinzugefügten Daten setzen
                        revisions[key].deltaAdded = value["http://eatld.et.tu-dresden.de/rmo#deltaAdded"][0].value;
                        // Revisionsnummer setzen
                        revisions[key].revisionNumber = value["http://eatld.et.tu-dresden.de/rmo#revisionNumber"][0].value;
                        // ID der eigentlichen Daten setzen
                        revisions[key].revisionOf = value["http://eatld.et.tu-dresden.de/rmo#revisionOf"][0].value;
                        // ID des Branches setzen, zu dem die Revision gehört
                        revisions[key].revisionOfBranch = value["http://eatld.et.tu-dresden.de/rmo#revisionOfBranch"][0].value;
                        break;
                    // Falls Branch
                    case "http://eatld.et.tu-dresden.de/rmo#Branch":
                        // Falls der Branch noch nicht im Datenmodell existiert muss er initialisiert werden
                        if (branches[key] == null) {
                            branches[key] = {};
                        }
                        // Name des Branch setzen
                        branches[key].label = value["http://www.w3.org/2000/01/rdf-schema#label"][0].value;
                        // ID der eigentlichen Daten setzen
                        branches[key].fullGraph = value["http://eatld.et.tu-dresden.de/rmo#fullGraph"][0].value;
                        // ID der Revision am Kopf des Branches setzen
                        branches[key].head = value["http://eatld.et.tu-dresden.de/rmo#references"][0].value;
                        // Falls noch keine Farbe für den Branch definiert ist, eine aus D3 laden
                        if (branches[key].color == null) {
                            branches[key].color = d3.rgb(colors(j)).brighter().toString();
                        }
                        break;
                    // Falls Tag
                    case "http://eatld.et.tu-dresden.de/rmo#ReferenceCommit":
                        // Tag initialisieren
                        referenceCommits[key] = {};
                        // Name des Tags setzen
                        referenceCommits[key].title = value["http://purl.org/dc/terms/title"][0].value;
                        // ID des Ersteller setzen
                        referenceCommits[key].wasAssociatedWith = value["http://www.w3.org/ns/prov#wasAssociatedWith"][0].value;
                        // Array für Zeitstempel initialisieren
                        referenceCommits[key].atTime = [];
                        // Array für Revisionen, die der Tag refereziert
                        referenceCommits[key].used = [];
                        // Alle Zeitstempel laden
                        for (k = 0; k < value["http://www.w3.org/ns/prov#atTime"].length; k++) {
                            referenceCommits[key].atTime.push(value["http://www.w3.org/ns/prov#atTime"][k].value);
                        }
                        // Alle referenzierten Revisionen laden
                        for (k = 0; k < value["http://www.w3.org/ns/prov#used"].length; k++) {
                            referenceCommits[key].used.push(value["http://www.w3.org/ns/prov#used"][k].value);
                        }
                        break;
                    // Falls Master
                    case "http://eatld.et.tu-dresden.de/rmo#Master":
                        // Falls der Masterbranch noch nicht als Branch vorliegt, muss er initialisiert werden
                        if (branches[key] == null) {
                            branches[key] = {};
                        }
                        // Alle Masterbranches sollen immer die gleiche Farbe haben
                        branches[key].color = "#5555ff";
                        break;

                }
            }

        });
        // Einstellungen für die Abstände zwischen den Knoten, Kanten und Level
        g.graph().ranksep = 10;
        g.graph().nodesep = 10;
        g.graph().edgesep = 10;
        g.graph().rankdir = "RL";
//        g.setGraph({
//          nodesep:10,
//          ranksep:10,
//          edgesep:10,
//          rankdir: "LR",
//          marginx: 10,
//          marginy: 10
//        });
        // Revisionen aus dem Datenmodell als Knoten erstellen
        // Dazu über alle Revisionen iterieren
        Object.keys(revisions).forEach(function (revision) {
            // Alle Daten können im D3 Modell hinterlegt werden
            var value = revisions[revision];
            var version_body;
            var version_head;
            // Konfigurationsparameter setzen
            // Falls die Revisionsnummer für den Kreis zu lang ist, muss sie gekürzt werden
            if (revisions[revision].revisionNumber.length > 5) {
                // Wenn gekürzt werden muss, bleiben die ersten drei Zeichen und das letzte stehen, der Rest wird durch .. erstetzt
                version_body = revisions[revision].revisionNumber.substr(0, 3) + ".." + revisions[revision].revisionNumber.substr(revisions[revision].revisionNumber.length - 1);
            } else {
                // Sonst die ganze Revisionsnummer als Anzeigenamen setzen
                version_body = revisions[revision].revisionNumber;
            }
            
            // Falls die Revision am Kopf des Branches steht
            if (branches[revisions[revision].revisionOfBranch].head == revision) {
              // Wird der Kreis komplett in der Branchfarbe gefüllt
            	version_head = branches[revisions[revision].revisionOfBranch].label+": ";
                value.style = "fill:" + branches[revisions[revision].revisionOfBranch].color + ";stroke:none;";

            	
            } else {
                // Sonst erhält er nur einen Rand in Branchfarbe
            	version_head = "version: ";
            	value.style = "stroke:" + branches[revisions[revision].revisionOfBranch].color + ";";
            }
            
//            var html = "<div class=version-node>";
//            html += "<span class="+status+">"+"<font color = grey>"+version_head+"</font>"+"</span>";
//            html += "<span class=name-label>"+"<font color = grey>"+version_body+"</font>"+"</span>";
//            html += "</div>";
            
            var html = version_head + version_body;
            
//            value.labelType = "html";
            value.label = html;
            // Höhe setzen
            value.height = 30;
            // Revisionen werden als Kreise dargestellt
            value.shape = "rect";

            // Knoten erzeugen
            g.setNode(revision, value);
        });
        // Commits aus dem Datenmodell als Kanten erstellen
        Object.keys(commits).forEach(function (commit) {
            // Für jede Revision, aus der der Commit erzeugt wurde muss eine Kante erstellt werden
            for (var i = 0; i < commits[commit].used.length; i++) {
                // Farbe für die Kante
                var color;
                // Falls der Commit nur von einer Revision stammt
                if (commits[commit].used.length == 1) {
                    // Wird als Farbe für die Kante die Revision genommen, die der Commit erzeugt hat
                    color = branches[revisions[commits[commit].generated].revisionOfBranch].color;
                } else {
                    // Ansonsten die Farbe der Ursprungsrevision
                    color = branches[revisions[commits[commit].used[i]].revisionOfBranch].color;
                }
                // Kante von der Ursprungsrevision zur Revision, die der Commit erzeugt hat, erzeugen
                g.setEdge(commits[commit].generated, commits[commit].used[i], {
                    // Mindestabstand 4
                    minlen: 4,
                    // Farben setzen
                    style: "stroke:" + color + ";fill:none;",
                    arrowheadStyle: "fill:" + color + ";stroke:" + color + ";",
                    // Rundere Linien
                    lineInterpolate: "basis"
                });
            }
        });
        
        
        //createTags();
        //show Branch and show Tag
        if (_showBranches) {
            createBranches();
        }
        // Falls erforderlich, Tags zum Anzeigen erzeugen
        if (_showTags) {
            createTags();
        }

        // Renderer erzeugen
        render = new dagreD3.render();

        // Zoom aus D3 und Verschiebung des Graphen über D3 ermöglichen
        zoom = d3.behavior.zoom().on("zoom", function () {
            inner.attr("transform", "translate(" + d3.event.translate + ")" +
            "scale(" + d3.event.scale + ")");
        });

        // Zoom an dei SVG binden
        svg.call(zoom);

        // Graphen rendern und ins DOM einfügen
        render(inner, g);
        
        

        // Graph skalieren und positionieren
        center();
        
        //color change on click
        d3.selectAll("g.node rect").on("click", function(d) {
            var c = d3.select(this).classed("doubled");
            d3.select(this).classed("doubled", !c);
        });
       
    });
    
};
