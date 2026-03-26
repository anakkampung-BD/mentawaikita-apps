// Colors
var chartGreen = 'rgba(140, 193, 82,1)';
var chartRed = 'rgba(218, 68, 83,1)';
var chartBlue = 'rgba(74, 137, 220,1)';
var chartMagenta = 'rgba(150, 122, 220,1)';
var chartBrown = 'rgba(170, 142, 105,1)';
var chartOrange = 'rgba(233, 87, 63,1)';
var chartMint = 'rgba(55, 188, 155,1)';

var chartGreen1 = 'rgba(140, 193, 82,0.5)';
var chartRed1 = 'rgba(218, 68, 83,0.5)';
var chartBlue1 = 'rgba(74, 137, 220,0.8)';


// Function to render a chart if the container exists
function renderChart(selector, options) {
	const chartContainer = document.querySelector(selector);
	if (chartContainer) {
		const chartInstance = new ApexCharts(chartContainer, options);
		chartInstance.render();
	} else {
		// Activate this if you want to track your charts in the DOM
		// console.warn(`Chart container with selector "${selector}" not found in the DOM.`);
	}
}

// Exchange Page Charts

// Expenses chart
renderChart("#chart-financial", {
	series: [
		{
			name: 'Expenses', 
			data: [1500, 1000, 1600, 1500, 700, 1650], // Moderate with a spike in April
		},
		{
			name: 'Income', 
			data: [1800, 850, 1900, 2000, 1000, 2500], // Fluctuating income with a high in May
		},
		{
			name: 'Saving',
			data: [1300, 500, 1300, 2700, 500, 1300], // Savings reflect income-expense differences
		},
	],
	colors: [chartRed1, chartGreen1, chartBlue1], // Red (Expenses), Green (Income), Blue (Saving)
	chart: {
		toolbar: { show: false },
		height: 250,
		width: '100%',
		type: 'area',
		background: 'transparent',
		zoom: { enabled: false }, // Disable zooming
		animations: { enabled: false }, // Disable animations for interactivity
		selection: { enabled: false }, // Disable dragging
	},
	legend: {
		show: false, // Disable the legend
	},
	grid: { 
		show: false, // Disable grid lines
		padding: {
			left: 0, // Add padding to the left
			right: 0, // Add padding to the right
			top: 0, // Add padding to the top
			bottom: 0, // Add padding to the bottom
		},
	},
	xaxis: {
		categories: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'], // 6 months
		labels: {
			show: false, // Hide X-axis labels
		},
		axisBorder: {
			show: false, // Hide X-axis border
		},
		axisTicks: {
			show: false, // Hide X-axis ticks
		},
	},
	yaxis: {
		labels: {
			show: false, // Hide Y-axis labels
		},
		axisBorder: {
			show: false, // Hide Y-axis border
		},
	},
	dataLabels: {
		enabled: false,
	},
	stroke: { width: 2 },
	tooltip: {
		enabled: false,
	},
});


// BTC Chart
renderChart("#chart-btc", {
	series: [{ name: 'series1', data: [41, 58, 98, 79] }],
	colors: ['#8CC152'],
	chart: {
		toolbar: { show: false },
		height: 80,
		width: 150,
		type: 'area'
	},
	grid: { show: false },
	xaxis: {
		labels: { show: false },
		axisBorder: { show: false },
		axisTicks: { show: false }
	},
	yaxis: { labels: { show: false } },
	dataLabels: { enabled: false },
	stroke: { width: 1 },
	tooltip: { enabled: false },
});

// ETH Chart
renderChart("#chart-eth", {
	series: [{ name: 'series1', data: [92, 93, 92, 91] }],
	colors: ['#BF263C'],
	chart: {
		toolbar: { show: false },
		height: 80,
		width: 150,
		type: 'area'
	},
	grid: { show: false },
	xaxis: {
		labels: { show: false },
		axisBorder: { show: false },
		axisTicks: { show: false }
	},
	yaxis: { labels: { show: false } },
	dataLabels: { enabled: false },
	stroke: { width: 1 },
	tooltip: { enabled: false },
});

// EUR Chart
renderChart("#chart-eur", {
	series: [{ name: 'series1', data: [192, 150, 170, 170] }],
	colors: ['#5D9CEC'],
	chart: {
		toolbar: { show: false },
		height: 80,
		width: 150,
		type: 'area'
	},
	grid: { show: false },
	xaxis: {
		labels: { show: false },
		axisBorder: { show: false },
		axisTicks: { show: false }
	},
	yaxis: { labels: { show: false } },
	dataLabels: { enabled: false },
	stroke: { width: 1 },
	tooltip: { enabled: false },
});

// Main Chart
renderChart("#chart-activity", {
	series: [14, 73, 31, 17, 15],
	colors: [chartRed, chartGreen, chartBlue, chartMint, chartMagenta],
	chart: {
		width: '320px',
		animations: { enabled: false },
		toolbar: { show: false },
		type: 'donut'
	},
	legend: {
		show: false,
		position: 'bottom'
	},
	grid: { show: false },
	xaxis: {
		labels: { show: false },
		axisBorder: { show: false },
		axisTicks: { show: false }
	},
	yaxis: { labels: { show: false } },
	dataLabels: { enabled: false },
	stroke: { width: 0 },
	tooltip: { enabled: false },
});

// Component Demo Chart
renderChart("#charts-demo-1", {
	chart: {
		type: 'area',
		toolbar: { show: false },
		animations: { enabled: false },
	},
	series: [
		{ name: 'Mobile', data: [30, 40, 45, 50, 49, 60, 70] },
		{ name: 'PWA', data: [40, 50, 65, 70, 89, 90, 95] },
	],
	fill: {
		type: "gradient",
		gradient: {
			shadeIntensity: 1,
			opacityFrom: 0.7,
			opacityTo: 0.9,
			stops: [0, 90, 100],
		},
	},
	xaxis: {
		categories: [1991, 1992, 1993, 1994, 1995, 1996, 1997],
	},
});