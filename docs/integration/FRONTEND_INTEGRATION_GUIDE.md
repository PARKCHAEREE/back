# 대시보드 프론트엔드 연동 가이드

**작성일**: 2026-06-06  
**버전**: 1.0  
**대상**: React/Vue.js 등 프론트엔드 팀

---

## 📋 목차
1. [초기화 단계](#1-초기화-단계)
2. [1초 폴링 구현](#2-1초-폴링-구현)
3. [그래프 렌더링](#3-그래프-렌더링)
4. [이상 탐지 처리](#4-이상-탐지-처리)
5. [샘플 코드](#5-샘플-코드)
6. [주의사항](#6-주의사항)

---

## 1. 초기화 단계

### 1-1. 토큰 확보
```typescript
// 로그인 후 토큰 획득
const token = localStorage.getItem('accessToken'); // 또는 sessionStorage
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};
```

### 1-2. 발전소 정보 조회
```typescript
// 사용자의 발전소 목록 조회
const response = await fetch('/api/v1/plants', {
  headers
});
const { data: plants } = await response.json();
const plantId = plants[0].id; // 첫 번째 발전소 사용
```

### 1-3. 초기 대시보드 데이터 로드
```typescript
// 초기 데이터 1회 조회 (화면 로드 시)
const initialData = await fetch(
  `/api/v1/plants/${plantId}/dashboard/timeline?range=DAY`,
  { headers }
);
const { data: dashboardData } = await initialData.json();

// 상태 저장
setState({
  plantId,
  range: 'DAY',
  virtualNow: dashboardData.virtualNow,
  actualSeries: dashboardData.actualSeries,
  predictionSeries: dashboardData.predictionSeries,
  anomalyMarkers: dashboardData.anomalyMarkers
});
```

---

## 2. 1초 폴링 구현

### 2-1. 재생 시작
```typescript
async function startPlayback() {
  const response = await fetch('/api/v1/simulation/playback/start', {
    method: 'POST',
    headers
  });
  const { data } = await response.json();
  console.log('Playback started:', data);
  // {
  //   "running": true,
  //   "tickSeconds": 1,
  //   "stepHours": 1,
  //   "virtualCurrentTime": "2026-03-15T13:00:00Z",
  //   "lastTickAt": "2026-03-15T13:00:00Z"
  // }
  
  startPolling(); // 폴링 시작
}
```

### 2-2. 1초 폴링 루프
```typescript
let pollingInterval = null;

function startPolling() {
  pollingInterval = setInterval(async () => {
    try {
      const response = await fetch(
        `/api/v1/plants/${plantId}/dashboard/timeline?range=${range}`,
        { headers }
      );
      
      if (!response.ok) {
        if (response.status === 401) {
          // 토큰 만료
          stopPolling();
          showLoginModal();
          return;
        }
        throw new Error(`HTTP ${response.status}`);
      }
      
      const { data: newData } = await response.json();
      
      // 데이터 업데이트 (변경 감지)
      if (newData.lastUpdatedAt !== lastUpdatedAt) {
        updateDashboard(newData);
        lastUpdatedAt = newData.lastUpdatedAt;
      }
      
    } catch (error) {
      console.error('Polling error:', error);
      // 에러 발생 시 3초 후 재시도
      setTimeout(startPolling, 3000);
    }
  }, 1000); // 1초마다 실행
}

function stopPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
  }
}

// 컴포넌트 언마운트 시
onBeforeUnmount(() => {
  stopPolling();
  fetch('/api/v1/simulation/playback/stop', { method: 'POST', headers });
});
```

---

## 3. 그래프 렌더링

### 3-1. 차트 라이브러리 선택
```
권장: Chart.js, ECharts, Recharts, Plotly.js
- 대용량 데이터: ECharts (최적화됨, 1000+ 점 처리 가능)
- React: Recharts (컴포넌트 기반)
- 일반: Chart.js (가벼움)
```

### 3-2. 데이터 구조
```typescript
interface TimelineResponse {
  plantId: number;
  range: 'DAY' | 'WEEK' | 'MONTH';
  virtualNow: string;          // ISO 8601 형식
  windowStart: string;         // 조회 범위 시작
  windowEnd: string;           // 조회 범위 종료
  forecastEnd: string;         // 예측 범위 종료
  lastUpdatedAt: string;
  
  actualSeries: {
    measuredAt: string;        // 타임스탐프 (X축)
    powerKw: number;           // 발전량 (Y축)
    temperature?: number;
    irradiance?: number;
    humidity?: number;
  }[];
  
  predictionSeries: {
    measuredAt: string;        // 타임스탐프 (X축)
    powerKw: number;           // 예측량 (Y축)
  }[];
  
  gapSeries: {
    measuredAt: string;
    prediction: number;
    actual: number;
    absoluteGap: number;       // |actual - prediction|
    gapRate: number;           // 백분율 (0.3 = 30%)
  }[];
  
  anomalyMarkers: {
    eventId: number;
    detectedAt: string;        // 이상 발생 시간
    type: 'POWER' | 'VISION';
    severity: 'LOW' | 'MEDIUM' | 'HIGH';
    status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
    summary: string;
  }[];
}
```

### 3-3. ECharts 예시 (권장)
```typescript
import * as echarts from 'echarts';

function renderChart(data) {
  const chartDom = document.getElementById('chart-container');
  const myChart = echarts.init(chartDom, 'light', { renderer: 'canvas' });

  const option = {
    title: {
      text: `발전소 타임라인 (${data.range})`
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        // 마우스 호버 시 표시할 정보
        let html = params[0].axisValue + '<br/>';
        params.forEach(item => {
          html += `${item.seriesName}: ${item.value.toFixed(2)} kW<br/>`;
        });
        return html;
      }
    },
    legend: {
      data: ['실측 발전량', '예측 발전량']
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: (value) => {
          const date = new Date(value);
          return date.toLocaleString('ko-KR', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '발전량 (kW)',
      min: 0
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%'
    },
    series: [
      {
        name: '실측 발전량',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#1f77b4' },
        data: data.actualSeries.map(d => [
          new Date(d.measuredAt).getTime(),
          d.powerKw
        ]),
        symbolSize: 3,
        z: 2
      },
      {
        name: '예측 발전량',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#ff7f0e' },
        data: data.predictionSeries.map(d => [
          new Date(d.measuredAt).getTime(),
          d.powerKw
        ]),
        symbolSize: 2,
        z: 1
      }
    ]
  };

  // 이상 탐지 마커 추가
  data.anomalyMarkers.forEach(anomaly => {
    const timestamp = new Date(anomaly.detectedAt).getTime();
    const color = anomaly.severity === 'HIGH' ? '#ff0000' : 
                  anomaly.severity === 'MEDIUM' ? '#ffaa00' : '#00aa00';
    
    option.series.push({
      name: `${anomaly.type} 이상 (${anomaly.severity})`,
      type: 'scatter',
      symbolSize: 12,
      itemStyle: { color },
      data: [[timestamp, null]], // Y값은 그래프 자동 계산
      tooltip: {
        formatter: () => anomaly.summary
      },
      z: 3
    });
  });

  myChart.setOption(option);
  return myChart;
}
```

### 3-4. 데이터 업데이트
```typescript
function updateDashboard(newData) {
  // 상태 업데이트
  setState({
    actualSeries: newData.actualSeries,
    predictionSeries: newData.predictionSeries,
    anomalyMarkers: newData.anomalyMarkers,
    virtualNow: newData.virtualNow
  });
  
  // 차트 업데이트 (재렌더링)
  renderChart(newData);
  
  // 기타 UI 업데이트
  updateSummary(newData);
}
```

---

## 4. 이상 탐지 처리

### 4-1. 이상 마커 클릭
```typescript
myChart.on('click', (params) => {
  if (params.seriesType === 'scatter') {
    // 이상 탐지 마커 클릭
    const anomaly = data.anomalyMarkers.find(
      a => new Date(a.detectedAt).getTime() === params.data[0]
    );
    
    if (anomaly) {
      showAnomalyDetail(anomaly);
    }
  }
});

function showAnomalyDetail(anomaly) {
  const modal = `
    <div class="anomaly-detail">
      <h3>${anomaly.summary}</h3>
      <p>이상 유형: ${anomaly.type}</p>
      <p>심각도: <span class="severity-${anomaly.severity}">${anomaly.severity}</span></p>
      <p>상태: ${anomaly.status}</p>
      <p>감지 시간: ${anomaly.detectedAt}</p>
      
      <button onclick="acknowledgeAnomaly(${anomaly.eventId})">
        확인함
      </button>
    </div>
  `;
  
  document.getElementById('modal').innerHTML = modal;
}

async function acknowledgeAnomaly(eventId) {
  const response = await fetch(
    `/api/v1/plants/${plantId}/anomalies/${eventId}/status`,
    {
      method: 'PATCH',
      headers: {
        ...headers,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'ACKNOWLEDGED' })
    }
  );
  
  if (response.ok) {
    alert('이상 현상을 확인했습니다.');
  }
}
```

### 4-2. 실시간 알림
```typescript
// 1초 폴링 후 새로운 이상 발생 시
function checkNewAnomalies(oldData, newData) {
  const newAnomalies = newData.anomalyMarkers.filter(
    a => !oldData.anomalyMarkers.find(o => o.eventId === a.eventId)
  );
  
  newAnomalies.forEach(anomaly => {
    if (anomaly.severity === 'HIGH') {
      // 브라우저 알림 표시
      showNotification({
        title: '⚠️ 발전량 이상 감지',
        message: anomaly.summary,
        type: 'error'
      });
      
      // 서버에서 이메일 발송됨 (자동)
    }
  });
}
```

---

## 5. 샘플 코드

### 5-1. React 전체 예시
```jsx
import React, { useState, useEffect } from 'react';
import * as echarts from 'echarts';

function Dashboard() {
  const [data, setData] = useState(null);
  const [range, setRange] = useState('DAY');
  const [isPlaying, setIsPlaying] = useState(false);
  const [chart, setChart] = useState(null);
  const token = localStorage.getItem('accessToken');
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };

  // 초기화
  useEffect(() => {
    const init = async () => {
      const plants = await fetch('/api/v1/plants', { headers });
      const { data: plantList } = await plants.json();
      const plantId = plantList[0].id;

      const dashboard = await fetch(
        `/api/v1/plants/${plantId}/dashboard/timeline?range=${range}`,
        { headers }
      );
      const { data: dashData } = await dashboard.json();
      setData(dashData);
      renderChart(dashData);
    };
    init();
  }, []);

  // 1초 폴링
  useEffect(() => {
    if (!isPlaying || !data) return;

    const interval = setInterval(async () => {
      try {
        const response = await fetch(
          `/api/v1/plants/${data.plantId}/dashboard/timeline?range=${range}`,
          { headers }
        );
        const { data: newData } = await response.json();
        setData(newData);
        chart && chart.setOption(generateChartOption(newData));
      } catch (error) {
        console.error(error);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [isPlaying, range]);

  const renderChart = (data) => {
    const chartDom = document.getElementById('chart');
    const myChart = echarts.init(chartDom);
    myChart.setOption(generateChartOption(data));
    setChart(myChart);
  };

  const generateChartOption = (data) => {
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['실측', '예측'] },
      xAxis: { type: 'time' },
      yAxis: { type: 'value' },
      series: [
        {
          name: '실측',
          type: 'line',
          data: data.actualSeries.map(d => [
            new Date(d.measuredAt).getTime(),
            d.powerKw
          ])
        },
        {
          name: '예측',
          type: 'line',
          data: data.predictionSeries.map(d => [
            new Date(d.measuredAt).getTime(),
            d.powerKw
          ])
        }
      ]
    };
  };

  const handleRangeChange = (newRange) => {
    setRange(newRange);
  };

  const handlePlayback = async () => {
    if (!isPlaying) {
      await fetch('/api/v1/simulation/playback/start', {
        method: 'POST',
        headers
      });
      setIsPlaying(true);
    } else {
      await fetch('/api/v1/simulation/playback/stop', {
        method: 'POST',
        headers
      });
      setIsPlaying(false);
    }
  };

  return (
    <div>
      <h1>발전소 대시보드</h1>
      
      <div style={{ marginBottom: '20px' }}>
        <button onClick={() => handleRangeChange('DAY')} disabled={isPlaying}>DAY</button>
        <button onClick={() => handleRangeChange('WEEK')} disabled={isPlaying}>WEEK</button>
        <button onClick={() => handleRangeChange('MONTH')} disabled={isPlaying}>MONTH</button>
        <button onClick={handlePlayback} style={{ marginLeft: '20px' }}>
          {isPlaying ? '⏸ 일시정지' : '▶ 재생'}
        </button>
      </div>

      {data && (
        <div>
          <p>현재 시간: {data.virtualNow}</p>
          <div id="chart" style={{ width: '100%', height: '500px' }}></div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
```

---

## 6. 주의사항

### 6-1. 성능 최적화
```
⚠️ 주의할 점:
- 실제 데이터가 크면 (1000+ 점) 차트 렌더링 느릴 수 있음
- 해결책:
  1) ECharts의 canvase 렌더러 사용
  2) 샘플링: 매 10개 점 중 1개만 표시
  3) 가상 스크롤: 화면 범위만 렌더링

예시:
const sampledData = data.actualSeries.filter((_, i) => i % 10 === 0);
```

### 6-2. 시간대 처리
```typescript
// ✅ 올바른 방법
const date = new Date(response.measuredAt); // ISO 8601 파싱
const formatted = date.toLocaleString('ko-KR');

// ❌ 틀린 방법
const timestamp = response.measuredAt;  // 문자열을 숫자로 사용 금지
```

### 6-3. 에러 처리
```typescript
async function safeTimelineCall() {
  try {
    const response = await fetch('/api/v1/plants/1/dashboard/timeline', {
      headers
    });
    
    switch (response.status) {
      case 200:
        return response.json();
      case 401:
        // 토큰 갱신 로직
        refreshToken();
        return safeTimelineCall(); // 재시도
      case 404:
        throw new Error('발전소를 찾을 수 없습니다');
      case 500:
        throw new Error('서버 오류. 나중에 다시 시도하세요');
      default:
        throw new Error(`Unknown error: ${response.status}`);
    }
  } catch (error) {
    console.error('Timeline fetch failed:', error);
    showErrorNotification(error.message);
  }
}
```

### 6-4. 메모리 누수 방지
```typescript
function useDashboard(plantId) {
  useEffect(() => {
    let pollingInterval = null;
    let chart = null;

    const startPolling = () => {
      pollingInterval = setInterval(async () => {
        // 폴링 로직
      }, 1000);
    };

    startPolling();

    // 정리 함수 (unmount 시 실행)
    return () => {
      clearInterval(pollingInterval);
      if (chart) {
        chart.dispose(); // ECharts 정리
      }
      // playback 정지
      fetch('/api/v1/simulation/playback/stop', { method: 'POST' });
    };
  }, [plantId]);
}
```

---

## 요약

| 단계 | 설명 | 시간 |
|------|------|------|
| 초기화 | 토큰, 발전소, 초기 데이터 로드 | ~ 1초 |
| 재생 시작 | `/playback/start` 호출 | ~ 100ms |
| 폴링 루프 | 1초마다 `/dashboard/timeline` 호출 | 1초 간격 |
| 그래프 업데이트 | 새 데이터로 차트 재렌더링 | ~ 50-100ms |
| 이상 처리 | 새 anomalyMarker 감지 및 표시 | 즉시 |
| 종료 | `/playback/stop` + 폴링 중단 | ~ 100ms |

**전체 시연 시간**: 약 1시간 11분 (5개월 데이터 × 1초 = 5개월)

---

**생성일**: 2026-06-06  
**관련 API**: `GET /dashboard/timeline`, `POST /playback/start`, `POST /playback/stop`

