/* 누적 그래프, 표 */
	$.ajax({
	type: "GET",
	url:"/totalFeedback",
	asyn:"true",
	dataType:"json",
	data:{ 
	},
	success:function(data){//통신 성공
	  //console.log("success data:"+data);
	  //console.log("data.u1" + data.u1);
	  //console.log("data.u2" + data.u2);
	  
	  // feedback_barchart
	  new Chart(document.getElementById("feedback_barchart"), {
	      type: 'bar',
	      data: {
	        labels: ['모양 오류', '색깔 오류', '그림/숫자 오류'],
	        datasets: [{
	          data: [data.u1, data.u2, data.u3],
	          label: 'feedback',
	          backgroundColor: [
	            '#C8E6C9',
	            '#BBDEFB',
	            '#FFFACD'
	          ],
	          barThickness: 75
	        }]
	      },
	      options: {
	        plugins: {
	          legend: { display: false },
	          tooltip: {
	            mode: 'index',
	            intersect: false,
	          },
	          interaction: {
	            mode: 'nearest',
	            intersect: true
	          }
	        },
	        scales: {
	          y: {
	            beginAtZero: true,
	            display: true,
	            ticks: {
	              stepSize: 1, // 눈금 간격 설정
	              callback: function(value) {
	                return value + '건';
	              }
	            }
	          }
	        }
	      }
	    }); // feedback_barchart
	    
	    $("#u1").text(data.u1 + "건");
	    $("#u2").text(data.u2 + "건");
	    $("#u3").text(data.u2 + "건");
	    
	    let total = parseInt(data.u1) + parseInt(data.u2);
	    $("#total").text(total + "건");
	    
	},
	error:function(data){//실패시 처리
	  console.log("error:"+data);
	    }
	  }); // ajax
	
	  
	  
	
/* 셀렉트박스, 월별 그래프 */
	let currentDate = new Date();
	let currentYear = currentDate.getFullYear(); // 현재 연도
	let currentMonth = currentDate.getMonth() + 1; // 현재 날짜의 월을 가져옴 (0부터 시작하므로 +1 필요)
	  
	// 셀렉트박스 옵션을 현재 월까지로
	for (var i = 1; i <= currentMonth; i++) {
		let optionValue = (i < 10 ? "0" : "") + i; // 10보다 작으면 앞에 0붙임
		let optionText = i + "월";
		$("#monthDropDown").append($("<option>").attr("value", optionValue).text(optionText));
	}
	  
	// 셀렉트박스 현재 월에 해당하는 값으로 초기화
	$("#monthDropDown").val(currentMonth.toString().padStart(2, "0")); // 두 자리로 만들기 위해 0으로 패딩
	  
	// 현재 연도와 현재 월을 조합하여 날짜 문자열 생성 (예: "2023-08-01")
	let formattedDate = currentYear + '-' + currentMonth + '-01';
	let dateList = []; // 날짜 리스트
	let u1List = []; // 오류1 리스트
	let u2List = []; // 오류2 리스트
	let u3List = []; // 오류3 리스트
	  
	// default 그래프 (현재 월에 해당하는 그래프)
	function defaultMonthlyGraph(uploadDate) {
	    $.ajax({
		    type: "GET",
		    url:"/monthlyFeedback",
		    asyn:"true",
		    dataType:"json",
		    data:{
		      uploadDate: formattedDate // 생성한 날짜 문자열을 전달
		    },
		    success:function(data){//통신 성공
		      //console.log("success data:"+data);    
		      for (var i = 0; i < data.length; i++) {
		        dateList.push(data[i].uploadDate);
		      }
		      for (var i = 0; i < data.length; i++) {
		        u1List.push(data[i].u1);
		      }
		      for (var i = 0; i < data.length; i++) {
		        u2List.push(data[i].u2);
		      }
		      
		      // 이전 차트 파괴
		      if (window.myChart) {
		          window.myChart.destroy();
		      }
		      // 새로운 차트 생성
		      let ctx = document.getElementById('feedback_linechart').getContext('2d');
		      window.myChart = new Chart(ctx, {
		        type: 'line',
		        data: {
		          labels: dateList,
		          datasets: [{
		            data: u1List,
		            label: '모양 오류',
		            borderColor: '#C8E6C9',
		            fill: false,
		            tension: 0
		          },
		          {
		            data: u2List,
		            label: '색깔 오류',
		            borderColor: '#BBDEFB',
		            fill: false,
		            tension: 0
		          },
		          {
		        	  data: u3List,
		        	  label: '그림/숫자 오류',
		        	  borderColor: '#FFFACD',
		        	  fill: false,
		        	  tension: 0
		          }]
		        },
		        options: {
		          plugins: {
		            legend: { display: false },
		            tooltip: {
		              mode: 'index',
		              intersect: false,
		            },
		            interaction: {
		              mode: 'nearest',
		              intersect: true
		            }
		          },
		          scales: {
		            y: {
		              display: true,
		              ticks: {
		                stepSize: 1, // 눈금 간격 설정
		                callback: function(value) {
		                  return value + '건';
		                }
		              }
		            }
		          }
		        }
		      }); // feedback_linechart        
		    }, // success
		    error:function(data){//실패시 처리
		      console.log("error:"+data);
		    }
	    }); // ajax            
	} // defaultMonthlyGraph
	
	    
	// 초기화 시 default그래프 표시
	defaultMonthlyGraph(formattedDate);
	  
	// 셀렉트박스 선택 시 그래프 변화
	$("#monthDropDown").change(function() {
		// 선택한 월 값
		let selectedMonth = $("#monthDropDown").val();
	    
		// 현재 연도와 선택한 월을 조합하여 날짜 문자열 생성 (예: "2023-08-01")
		let currentDate = new Date();
		let currentYear = currentDate.getFullYear(); // 현재 연도
		let formattedDate = currentYear + '-' + selectedMonth + '-01';
		
		let dateList = []; // 날짜 리스트
		let u1List = []; // 오류1 리스트
		let u2List = []; // 오류2 리스트
		let u3List = []; // 오류3 리스트
	
		$.ajax({
			type: "GET",
			url:"/monthlyFeedback",
			asyn:"true",
			dataType:"json",
			data:{
			  uploadDate: formattedDate // 생성한 날짜 문자열을 전달
			},
			success:function(data){//통신 성공
			  //console.log("success data:"+data);    
			  for (var i = 0; i < data.length; i++) {
			    dateList.push(data[i].uploadDate);
			  }
			  for (var i = 0; i < data.length; i++) {
			    u1List.push(data[i].u1);
			  }
			  for (var i = 0; i < data.length; i++) {
			    u2List.push(data[i].u2);
			  }
			  
			  // 이전 차트 파괴
			  if (window.myChart) {
			      window.myChart.destroy();
			  }
			  // 새로운 차트 생성
			  let ctx = document.getElementById('feedback_linechart').getContext('2d');
			  window.myChart = new Chart(ctx, {
			    type: 'line',
		        data: {
			          labels: dateList,
			          datasets: [{
			            data: u1List,
			            label: '모양 오류',
			            borderColor: '#C8E6C9',
			            fill: false,
			            tension: 0
			          },
			          {
			            data: u2List,
			            label: '색깔 오류',
			            borderColor: '#BBDEFB',
			            fill: false,
			            tension: 0
			          },
			          {
			        	  data: u3List,
			        	  label: '그림/숫자 오류',
			        	  borderColor: '#FFFACD',
			        	  fill: false,
			        	  tension: 0
			          }]
			    },
			    options: {
			      plugins: {
			        legend: { display: false },
			        tooltip: {
			          mode: 'index',
			          intersect: false,
			        },
			        interaction: {
			          mode: 'nearest',
			          intersect: true
			        }
			      },
			      scales: {
			        y: {
			          display: true,
			          ticks: {
			            stepSize: 1, // 눈금 간격 설정
			            callback: function(value) {
			              return value + '건';
			            }
			          }
			        }
			      }
			    }
			  }); // feedback_linechart
			  
			}, // success
			error:function(data){//실패시 처리
			  console.log("error:"+data);
			}
		}); // ajax
	     
	}); // monthDropDown

	
	
	
/* 아코디언 */
  document.addEventListener("DOMContentLoaded", function () {
      const accordionItems = document.querySelectorAll(".accordion-item");

      // 초기 첫 번째 아코디언 열기
      accordionItems[0].classList.add("open");

      accordionItems.forEach(item => {
        const header = item.querySelector(".accordion-header");

        header.addEventListener("click", () => {
          if (item.classList.contains("open")) {
            item.classList.remove("open");
          } else {
            // 열린 아코디언이 있을 경우 닫기
            accordionItems.forEach(otherItem => {
              if (otherItem !== item && otherItem.classList.contains("open")) {
                otherItem.classList.remove("open");
              }
            });
            item.classList.add("open");
          }
        });
      });
    });
