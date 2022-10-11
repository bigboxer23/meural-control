# meural-control

This project provides ability to push content to a Meural display from an external URL.

It leverages Meural's API which their web-interface and mobile app run on. Documentation can be found <a href="https://documenter.getpostman.com/view/1657302/RVnWjKUL#intro/">here.</a><br>

There is also an http server that runs directly on the Meural device, which is used to post content directly to.

Available calls:<br>
`/remote/identify/`<br>
`/remote/get_galleries_json/`<br>
`/remote/get_gallery_status_json/`<br>
`/remote/get_frame_items_by_gallery_json/`<br>
`/remote/get_wifi_connections_json/`<br>
`/remote/get_backlight/`<br>
`/remote/control_check/sleep/`<br>
`/remote/control_check/video/`<br>
`/remote/control_check/als/`<br>
`/remote/control_check/system/`<br>
`/remote/control_command/boot_status/image/`<br>
`/remote/control_command/set_key/`<br>
`/remote/control_command/set_backlight/`<br>
`/remote/control_command/suspend`<br>
`/remote/control_command/resume`<br>
`/remote/control_command/set_orientation/`<br>
`/remote/control_command/change_gallery/`<br>
`/remote/control_command/change_item/`<br>
`/remote/control_command/rtc/`<br>
`/remote/control_command/language/`<br>
`/remote/control_command/country/`<br>
`/remote/control_command/als_calibrate/off/`<br>
`/remote/control_command_post/connect_to_new_wifi/`<br>
`/remote/control_command_post/connect_to_exist_wifi/`<br>
`/remote/control_command_post/connect_to_hidden_wifi/`<br>
`/remote/control_command_post/delete_wifi_connection/`<br>
`/remote/postcard/`<br>