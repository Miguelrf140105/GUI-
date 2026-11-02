import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';
getTestBed().initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
declare const require: {
  context(path: string, deep: boolean, filter: RegExp): { keys(): string[]; (id: string): any };
};
const context = require.context('./', true, /\.spec\.ts$/);
context.keys().forEach(context);
