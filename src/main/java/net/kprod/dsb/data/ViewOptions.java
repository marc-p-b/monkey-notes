package net.kprod.dsb.data;

import net.kprod.dsb.data.enums.ViewOptionsCompletionStatus;

public class ViewOptions {

        public static ViewOptions all() {
            return new ViewOptions()
                    .setCompletionStatus(ViewOptionsCompletionStatus.all);
        }

        private ViewOptionsCompletionStatus completionStatus;

        public ViewOptions setCompletionStatus(ViewOptionsCompletionStatus completionStatus) {
            this.completionStatus = completionStatus;
            return this;
        }

        public ViewOptionsCompletionStatus getCompletionStatus() {
            return completionStatus;
        }
    }