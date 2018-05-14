
set theJar="bin\Outlanded-release-unsigned.apk"
set keyStorePath="C:\\Program Files (x86)\\Java\\jre6\\lib\\security\\cacerts"
set keyAlias="ibiskuv klic pro androida"

jarsigner -verbose -keystore %keyStorePath% %theJar% %keyAlias%