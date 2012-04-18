package mambo;

class ImportSalesForceObject {
    def typeMap = buildTypeMap();
    def inputDir;
    def outputDir;
    def pkgName;

    ImportSalesForceObject(String inputDir, String outputDir, String pkgName) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.pkgName = pkgName;
    }

    void importObjects() {
        File inDir = new File(inputDir);
        File outDir = new File(outputDir);
        
        inDir.eachFileMatch(~/.*\.object$/, {f -> println(transformFile(f, pkgName))})

    }

    String removeSuffix(str, suffix) {
        if (str.endsWith(suffix)) {
            return str.substring(0, str.lastIndexOf(suffix));
        }
        else {
            return str;
        }
    }
    
    String camelCase(str, firstIsLower) {
        def comps = str.tokenize("_")
        def concatName = comps.inject("") {acc, val -> if (acc.equals("") && firstIsLower) {
                acc + val.toLowerCase()
            }
            else {
                acc + val.toLowerCase().capitalize()
            }};
        return concatName;
    }
    
    String transformApiName(name) {
        def newName = removeSuffix(name, "__c");
        return camelCase(newName, true);
    }
    
    Map buildTypeMap() {
        def map = [:];
        map.put("number", {typeInfo -> if (typeInfo.scale > 0) {
		"BigDecimal"
                }
                else {
		"BigInteger"
                }});
        map.put("checkbox", {"boolean"});
        map.put("text", {"String"});
        map.put("percent", {"BigDecimal"});
        map.put("phone", {"String"});
        return map;
    }
    
    String transformType(type) {
        def transformer = typeMap[type?.name?.toLowerCase()];
        if (transformer == null) {
            return type?.name;
        }
        else {
            return transformer(type);
        }
    }
    
    String transform(doc, objectName, pkgName) {
        def fields = doc.fields.collect({[apiName:it.fullName.text(), 
                    type:[name: it.type.text(), precision: it.precision.text(), scale: it.scale.text()]]});
        def StringBuilder builder = new StringBuilder();

        builder.append("package " + pkgName + "\n\n");
        builder.append("class " + camelCase(removeSuffix(objectName, "__c"), false) + " {\n\n");
        fields.each({builder.append("   " + transformType(it.type) + " " + transformApiName(it.apiName) + "\n")});
        builder.append("\n}\n");
        return builder.toString();
    }
    
    String transformFile(myFile, pkgName) {
        def baseName = removeSuffix(myFile.getName(), ".object");
        def doc = new XmlSlurper().parse(myFile); 
        return transform(doc, baseName, pkgName);
    }
    
}

i = new ImportSalesForceObject("C:/projects/schoolforce/wkspc/SchoolforceDev_Mamboking/src/objects", "", "com.schoolforce.domain");
print (i.importObjects());
