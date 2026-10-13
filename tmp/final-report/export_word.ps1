$reportWord = New-Object -ComObject Word.Application
$reportWord.Visible = $false
$reportWord.DisplayAlerts = 0
try {
 $reportDoc = $reportWord.Documents.Open('D:\SLIIT\Year 02\Semester 01\SE\Project\identity-issuance-service(Project)\output\final-report\2026-Y2-S1-MTR-02_nusla_final.docx', $false, $false)
 $reportDoc.Fields.Update() | Out-Null
 foreach ($reportToc in $reportDoc.TablesOfContents) { $reportToc.Update() }
 $reportDoc.Repaginate()
 $reportDoc.SaveAs2('D:\SLIIT\Year 02\Semester 01\SE\Project\identity-issuance-service(Project)\tmp\final-report\word-refreshed.docx', 16)
 $reportDoc.ExportAsFixedFormat('D:\SLIIT\Year 02\Semester 01\SE\Project\identity-issuance-service(Project)\output\final-report\2026-Y2-S1-MTR-02.pdf', 17)
 Write-Output ('Rendered pages: ' + $reportDoc.ComputeStatistics(2))
 $reportDoc.Close(0)
} finally { $reportWord.Quit() }
