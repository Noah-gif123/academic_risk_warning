# =====================================================================
# rag-blind-eval.ps1 —— 盲测集 vs 自编集 对比（任务 B）
#
# 用途：把"开发者自编的评测集"和"别人出的盲测题"分档跑一遍，
#       用差距说明模型没有过度拟合自己的题。判读标准见 v2.0/ai/rag-eval.md §11。
#
# 用法：
#   powershell -NoProfile -File v2.0\tool\rag-blind-eval.ps1
#   powershell -NoProfile -File v2.0\tool\rag-blind-eval.ps1 -Base http://127.0.0.1:8080 -K 4
#
# 前置：后端在跑；盲测题已按 §11 录入 ai_rag_eval_case（note='blind'）。
# =====================================================================
param(
    [string]$Base = 'http://127.0.0.1:8080',
    [string]$LoginNo = 'admin001',
    [string]$Password = '000000',
    [int]$K = 4,
    [string]$Mode = 'HYBRID'
)

$ErrorActionPreference = 'Stop'

function Invoke-Api($url, $body, $token, $method) {
    $json = if ($null -ne $body) { $body | ConvertTo-Json -Depth 8 } else { '{}' }
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $req = [System.Net.HttpWebRequest]::Create($url)
    $req.Method = $method
    $req.ContentType = 'application/json; charset=utf-8'
    if ($token) { $req.Headers.Add('Authorization', $token) }
    $req.ContentLength = $bytes.Length
    $s = $req.GetRequestStream(); $s.Write($bytes, 0, $bytes.Length); $s.Close()
    try {
        $resp = $req.GetResponse()
    } catch [System.Net.WebException] {
        $r = $_.Exception.Response
        if (-not $r) { throw }
        $sr = New-Object System.IO.StreamReader($r.GetResponseStream())
        return @{ code = [int]$r.StatusCode; body = $sr.ReadToEnd() }
    }
    $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
    return @{ code = 200; body = $sr.ReadToEnd() }
}

Write-Host "登录 $LoginNo ..." -ForegroundColor Cyan
$login = Invoke-Api "$Base/api/auth/login" @{ loginNo = $LoginNo; password = $Password } $null 'POST'
if ($login.code -ne 200) { throw "登录失败：$($login.body)" }
$token = ($login.body | ConvertFrom-Json).data.token

function Run-One($note, $label) {
    $q = "mode=$Mode&k=$K"
    if ($note) { $q += "&note=$note" }
    $r = Invoke-Api "$Base/api/admin/ai/rag-eval?$q" $null $token 'POST'
    $d = $null
    try { $d = ($r.body | ConvertFrom-Json) } catch { }
    if (-not $d -or $d.code -ne 0) {
        return [pscustomobject]@{
            label = $label; cases = 0; recall = $null; mrr = $null; ndcg = $null
            refuse = $null; falseRefuse = $null; ms = $null; err = $d.message
        }
    }
    return [pscustomobject]@{
        label      = $label
        cases      = $d.data.cases
        recall     = $d.data.recallAtK
        mrr        = $d.data.mrr
        ndcg       = $d.data.ndcgAtK
        refuse     = $d.data.refuseAccuracy
        falseRefuse = $d.data.falseRefuse
        ms         = $d.data.avgMs
        err        = $null
    }
}

Write-Host "`n跑自编集（note=seed38）..." -ForegroundColor Cyan
$self = Run-One 'seed38' '自编集'
Write-Host "跑盲测集（note=blind）..." -ForegroundColor Cyan
$blind = Run-One 'blind' '盲测集'

Write-Host "`n==== 盲测对比（模式 $Mode，k=$K）====" -ForegroundColor Green
$rows = @($self, $blind)
$rows | Format-Table -AutoSize @(
    @{ n = '数据集'; e = { $_.label } },
    @{ n = '条数'; e = { $_.cases } },
    @{ n = 'recall@k'; e = { $_.recall } },
    @{ n = 'MRR'; e = { $_.mrr } },
    @{ n = 'nDCG@k'; e = { $_.ndcg } },
    @{ n = '拒答准确率'; e = { $_.refuse } },
    @{ n = '误拒率'; e = { $_.falseRefuse } },
    @{ n = '耗时ms'; e = { $_.ms } }
)

if ($blind.err) {
    Write-Host "`n盲测集暂不可用：$($blind.err)" -ForegroundColor Yellow
    Write-Host "按 v2.0\ai\rag-eval.md §11 录入盲测题后重跑即可。" -ForegroundColor Yellow
    exit 0
}
if ($blind.cases -eq 0) {
    Write-Host "`n盲测集为空，先录入用例。" -ForegroundColor Yellow
    exit 0
}

$gap = $null
if ($null -ne $self.recall -and $null -ne $blind.recall) {
    $gap = [math]::Round(($self.recall - $blind.recall), 4)
}
Write-Host "`nrecall 差距（自编 - 盲测）：$gap" -ForegroundColor Green
if ($null -ne $gap) {
    if ([math]::Abs($gap) -le 0.10) {
        Write-Host "判读：差距 ≤ 0.10，说明检索没有过度拟合自编题，可直接写进答辩材料。" -ForegroundColor Green
    } else {
        Write-Host "判读：差距 > 0.10，说明自编题偏乐观；请按盲测集的失分题型补语料或调检索参数。" -ForegroundColor Yellow
    }
}
