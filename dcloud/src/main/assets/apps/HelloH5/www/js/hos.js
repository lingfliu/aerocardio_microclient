(function($, hos) {
	hos.app = {
		basePath: "/wxyyghsys/"
			//http://www.whghyy.com/
			///wxyyghsys/
	};

	hos.dept = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_findDeptList.do',
			DATA : {deptstr:""}
	}
	
	hos.deptForToday = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_findDeptListForToday.do',
			DATA : {deptstr:""}
	}
	
	hos.doctor = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_findDoctorList.do',
			DATA : {doctorstr:""}
	}
	
	hos.doctorForToday = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_doctorListForToday.do',
			DATA : {doctorstr:""}
	}

	hos.doctortime = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_findDoctorPb.do',
			DATA : {doctortimestr:""}
	}
	
	hos.addOrders = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_addOrder.do',
			DATA : {ordersStr:""}
	}
	
	hos.queryOrders = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_queryOrders.do',
			DATA : {ordersStr:""}
	}
	
	hos.payOrder = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_payOrder.do',
			DATA : {ordersStr:""}
	}
	
	hos.cancelOrder = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_cancelOrder.do',
			DATA : {ordersStr:""}
	}
	
	hos.lockRegToday = {
			TYPE : "GET",
			URL	: hos.app.basePath  + 'webServer_lockRegToday.do',
			DATA : {ordersStr:""}
	}
	

	
	hos.storagepar = {
		storage_code: {
			loginToken: "NO1_ERP_LGTOKEN",
			delstatus: "NO1_ERP_DELSTATUS",
			userInfo: "NO1_ERP_UEINFO",
			sid: "NO1_ERP_SID",
			spasword: "NO1_ERP_SPASWORD",
			url:"hrefurl"
		}
	}
	
	hos.storage = {
		set: function(key, value) {
			window.localStorage.setItem(key, value);
		},
		get: function(key) {
			return window.localStorage.getItem(key);
		},
		remove: function(key) {
			window.localStorage.removeItem(key);
		},
		clear: function() {
			window.localStorage.clear();
		},
		update: function(key, value) {
			window.localStorage.removeItem(key);
			window.localStorage.setItem(key, value);
		}
	}
	
	hos.patientpar = {
		patient_code: {
			cardno: "NO1_ERP_CARDNO",
			realname: "NO1_ERP_REALNAME",
			phone: "NO1_ERP_PHONE",
			idcard: "NO1_ERP_IDCARD",
			isBaundCard: "IS_BAUND_CARD",
			code: "CODE",
			patid:"PATID"
		}
	}
	

	/**
	 * 全局 类名方法  open 的 tap 事件 
	 * dom 额外属性 ：
	 * 	@param data-href  用于跳转路径［必填］
	 * 	@param data-extras  用于页面传值［选填］
	 **/
	hos.extras = {
		get: function(key) {
			if(window.plus) {
				return plus.webview.currentWebview().extras[key];
			} else {
				return hos.storage.get(key);
			}
		},
		set: function(href, extras) {
			hos.storage.set(href, extras);
		},
		remove: function(key) {
			hos.storage.remove(key);
		}
	}

	hos.isEmptyObject = function(e) {
		var t;
		for(t in e)
			return !1;
		return !0
	}

	hos.open = {
		openWindow: function(options) {
			mui.openWindow(options);
		},
		/*返回后刷新页面*/
		backReload: function() {

		}
	}

	mui("body").on("tap", ".NO1-open", function() {
		var href = this.getAttribute("data-href");
		if(!href) return false;
		var extras = this.getAttribute("data-extras") || {};

		if(window.plus) {
			hos.open.openWindow({
				url: href,
				extras: {
					extras: extras
				}
			});
		} else {
			//alert(extras);
			hos.extras.remove(href); //清除数据
			if(!hos.isEmptyObject(extras)) {
				hos.extras.set(href, extras);
			}
			hos.open.openWindow({
				url: href
			});
		}
	});

	hos.validation = {
		number: {
			validator: function(value) {
				return isNumber(value);
			},
			message: '请输入正确的数字.'
		},
		phonecheck: {
			validator: function(value) {
				return /^(1[34578])[0-9]{9}$/.test(value);
			},
			message: '请输入正确的手机号码.'
		},
		landline: {
			validator: function(value) {
				return /^(0[0-9]{2,3}-)?([2-9][0-9]{6,7})(-[0-9]{1,4})?$/.test(value);
			},
			message: '请输入正确的固话号码,如:010-88888888-88'
		},
		password: {
			validator: function(value) {
				return /^[a-zA-Z0-9_]{6,16}$/.test(value);
			},
			message: '密码为6-16位大小写字母、数字和下划线至少选择两种类型的组合'
		},
		equals: {
			validator: function(value, param) {
				return value == param;
			}
		},
		minLength: {
			validator: function(value, param) {
				return value.length >= param;
			}
		},
		maxLength: {
			validator: function(value, param) {
				return value.length <= param;
			}
		},
		isCardNo: {
			validator: function(value) {
				return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(value);
			}
		},
		isNull: {
			validator: function(value) {
				return(value && !value.replace(/(^s*)|(s*$)/g, "").length == 0);
			}
		},
		/* 是否为正数 */
		isPositive: {
			validator: function(num) {
				var reg = /^\d+(?=\.{0,1}\d+$|$)/;
				if(reg.test(num)) return true;
				return false;
			}
		}
	}
}(mui, window.hos = {}));