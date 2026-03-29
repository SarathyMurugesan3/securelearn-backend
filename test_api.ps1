$body = '{
    "email": "admin@company.com",
    "password": "SecurePassword123!",
    "fingerprint": "debug-fingerprint"
}'
$response = Invoke-RestMethod -Uri 'https://securelearn-backend.onrender.com/api/auth/login' -Method Post -Body $body -ContentType 'application/json' -ErrorAction Stop

$token = $response.accessToken
Write-Host "Token: $token"

try {
    $stats = Invoke-RestMethod -Uri 'https://securelearn-backend.onrender.com/api/admin/dashboard/stats' -Method Get -Headers @{ 'Authorization' = "Bearer $token" }
    $stats | ConvertTo-Json
} catch {
    Write-Host $_.Exception.Response.StatusCode.value__
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $reader.ReadToEnd()
}
