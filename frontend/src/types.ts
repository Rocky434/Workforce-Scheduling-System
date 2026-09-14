export type Role='EMPLOYEE'|'OWNER'
export interface User{id:number;email:string;displayName:string;role:Role}
export interface CalendarDay{date:string;dayType:'WORKDAY'|'WEEKEND'|'HOLIDAY';holidayName:string|null;schedulable:boolean;assignedCount:number;selected:boolean}
export interface MonthView{month:string;selectedCount:number;minimumDays:number;maximumDays:number;dailyCapacity:number;days:CalendarDay[]}
export interface EmployeeStat{employeeId:number;displayName:string;monthlyDays:number;yearlyDays:number}
export interface Assignment{date:string;employeeId:number;displayName:string}
export interface Dashboard{month:string;employees:EmployeeStat[];assignments:Assignment[]}
