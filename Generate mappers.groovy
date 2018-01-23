import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import groovy.json.JsonOutput

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

typeMapping = [
        (~/(?i)int/)                      : "long",
        (~/(?i)float|double|decimal|real/): "double",
        (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
        (~/(?i)date/)                     : "java.sql.Date",
        (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def tableName = table.getName()
  def fields = calcFields(table)
  new File(dir, className + "Repository.xml").withPrintWriter { out -> generate(out, className, fields,tableName) }
}

def generate(out, className, fields ,tableName ) {
  out.println "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
  out.println "<mapper>"
  out.println ""
  out.println ""
  out.println "<resultMap id=\"$tableName\" type=\"com.xiaohongchun.member.db.pg.${className}Repository\">"
  out.println ""


  fields.each() {
    out.println " <result property=\"${underscoreToCamelCase(it.name)}\" column=\"${it.name}\"/> "
  }
  out.println "</resultMap>"
  out.println "</mapper>"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name : col.getName(),
                       type : typeStr,
                       annos: col]]
  }
}

def javaName(str, capitalize) {
  def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
          .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}


String underscoreToCamelCase(String underscore){
  if(!underscore || underscore.isAllWhitespace()){
    return ''
  }
  return underscore.replaceAll(/_\w/){ it[1].toUpperCase() }
}