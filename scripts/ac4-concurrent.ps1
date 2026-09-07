param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$VenueId = 2,
    [string]$StartTime = "18:00",
    [string]$EndTime = "19:00",
    [int]$Count = 20
)

$ErrorActionPreference = "Stop"
$date = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$bodyJson = (@{
    venueId   = $VenueId
    date      = $date
    startTime = $StartTime
    endTime   = $EndTime
    purpose   = "AC4"
} | ConvertTo-Json -Compress)

Write-Host "slot = venue $VenueId $date $StartTime-$EndTime"
Write-Host "logging in $Count users..."

$tokens = @()
for ($i = 1; $i -le $Count; $i++) {
    $loginJson = '{"username":"stress' + $i + '","password":"123456"}'
    $loginFile = Join-Path $env:TEMP "ac4-login-$i.json"
    [System.IO.File]::WriteAllText($loginFile, $loginJson)
    $loginResp = curl.exe -s -H "Content-Type: application/json" --data-binary "@$loginFile" "$BaseUrl/api/auth/login"
    $loginObj = $loginResp | ConvertFrom-Json
    if ($loginObj.code -ne "OK") {
        throw "login stress$i failed: $loginResp"
    }
    $tokens += $loginObj.data.token
}

Add-Type -AssemblyName System.Net.Http

$handler = [System.Net.Http.HttpClientHandler]::new()
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(30)
$uri = [Uri]::new("$BaseUrl/api/reservations")

$tasks = foreach ($token in $tokens) {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, $uri)
    $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $token)
    $request.Content = [System.Net.Http.StringContent]::new($bodyJson, [Text.Encoding]::UTF8, "application/json")
    $client.SendAsync($request)
}

Write-Host "firing $Count requests..."
[System.Threading.Tasks.Task]::WaitAll($tasks)

$ok = 0
$conflict = 0
$other = 0
foreach ($task in $tasks) {
    $response = $task.Result
    $text = $response.Content.ReadAsStringAsync().Result
    $code = ($text | ConvertFrom-Json).code
    if ($code -eq "OK") { $ok++ }
    elseif ($code -eq "CONFLICT") { $conflict++ }
    else {
        $other++
        Write-Host "other: $code $($response.StatusCode)"
    }
}

Write-Host "OK=$ok CONFLICT=$conflict OTHER=$other"
if ($ok -ne 1) {
    Write-Host "AC4 FAILED: expected exactly 1 success"
    exit 1
}
Write-Host "AC4 HTTP check passed (confirm DB count next)"
exit 0
